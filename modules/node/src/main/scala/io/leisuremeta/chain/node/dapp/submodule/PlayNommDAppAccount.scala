package io.leisuremeta.chain
package node
package dapp
package submodule

import java.time.temporal.ChronoUnit

import cats.{~>, Monad}
import cats.arrow.FunctionK
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.bifunctor.*
import cats.syntax.eq.*
import cats.syntax.traverse.*

import api.model.{
  Account,
  AccountData,
  AccountSignature,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash.ops.*
import lib.crypto.Recover.ops.*
import lib.datatype.Utf8
import lib.merkle.MerkleTrieState

object PlayNommDAppAccount:
  def apply[F[_]: Concurrent: PlayNommState](
      tx: Transaction.AccountTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] =
    tx match
      case ca: Transaction.AccountTx.CreateAccount =>
        for
          accountInfoOption <- getAccountInfo(ca.account)
          _ <- checkExternal(
            accountInfoOption.isEmpty,
            s"${ca.account} already exists",
          )
          _ <- checkExternal(
            sig.account == ca.account ||
              Some(sig.account) == ca.guardian,
            s"Signer ${sig.account} is neither ${ca.account} nor its guardian",
          )
          initialPKS <- getPKS(sig, ca)
          keyInfo = PublicKeySummary.Info(
            addedAt = ca.createdAt,
            description =
              Utf8.unsafeFrom(s"automatically added at account creation"),
            expiresAt = Some(ca.createdAt.plus(40, ChronoUnit.DAYS)),
          )
          _ <-
            if Option(sig.account) === ca.guardian then unit
            else
              PlayNommState[F].account.key
                .put((ca.account, initialPKS), keyInfo)
                .mapK(PlayNommDAppFailure.mapInternal {
                  s"Fail to put account key ${ca.account}"
                })
          accountData = AccountData(
            guardian = ca.guardian,
            ethAddress = ca.ethAddress,
            lastChecked = ca.createdAt,
          )
          _ <- PlayNommState[F].account.name
            .put(ca.account, accountData)
            .mapK(PlayNommDAppFailure.mapInternal {
              s"Fail to put account ${ca.account}"
            })
          _ <- ca.ethAddress
            .fold(StateT.empty[EitherT[F, String, *], MerkleTrieState, Unit]): ethAddress =>
              PlayNommState[F].account.eth.put(ethAddress, ca.account)
            .mapK:
              PlayNommDAppFailure.mapInternal(s"Fail to update eth address ${ca.ethAddress}")
            
        yield TransactionWithResult(Signed(sig, ca))(None)

      case ua: Transaction.AccountTx.UpdateAccount =>
        for
          accountData <- verifySignature(sig, ua)
          _ <- checkExternal(
            sig.account == ua.account ||
              Some(sig.account) == accountData.guardian,
            s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
          )
          _ <- PlayNommState[F].account.name
            .put(
              ua.account,
              accountData
                .copy(guardian = ua.guardian, ethAddress = ua.ethAddress),
            )
            .mapK(PlayNommDAppFailure.mapInternal {
              s"Fail to put account ${ua.account}"
            })
          _ <- ua.ethAddress
            .fold(StateT.empty[EitherT[F, String, *], MerkleTrieState, Unit]): ethAddress =>
              PlayNommState[F].account.eth.put(ethAddress, ua.account)
            .mapK:
              PlayNommDAppFailure.mapInternal(s"Fail to update eth address ${ua.ethAddress}")
        yield TransactionWithResult(Signed(sig, ua))(None)

      case ap: Transaction.AccountTx.AddPublicKeySummaries =>
        for
          accountData <- verifySignature(sig, ap)
          _ <- checkExternal(
            sig.account == ap.account ||
              Some(sig.account) == accountData.guardian,
            s"Signer ${sig.account} is neither ${ap.account} nor its guardian",
          )
          timeToCheck = accountData.lastChecked
            .plus(10, ChronoUnit.DAYS)
            .compareTo(ap.createdAt) < 0
          toRemove <-
            if !timeToCheck then pure(Vector.empty)
            else
              PlayNommState[F].account.key
                .from(ap.account.toBytes)
                .flatMapF { stream =>
                  stream
                    .filter { case (_, info) =>
                      info.expiresAt match
                        case None => false
                        case Some(time) =>
                          time
                            .plus(40, ChronoUnit.DAYS)
                            .compareTo(ap.createdAt) < 0
                    }
                    .map { case ((account, pks), info) =>
                      pks -> info.description
                    }
                    .compile
                    .toVector
                }
                .mapK(PlayNommDAppFailure.mapInternal {
                  s"Fail to get PKSes of account ${ap.account}"
                })
          _ <- toRemove.traverse { case (pks, _) =>
            PlayNommState[F].account.key
              .remove((ap.account, pks))
              .mapK(PlayNommDAppFailure.mapInternal {
                s"Fail to remove old PKS $pks from account ${ap.account}"
              })
          }
          _ <- ap.summaries.toSeq.traverse { case (pks, description) =>
            val expiresAt =
              if description === Utf8.unsafeFrom("permanent") then None
              else Some(ap.createdAt.plus(40, ChronoUnit.DAYS))
            val keyInfo = PublicKeySummary.Info(
              addedAt = ap.createdAt,
              description = description,
              expiresAt = expiresAt,
            )
            PlayNommState[F].account.key
              .put((ap.account, pks), keyInfo)
              .mapK(PlayNommDAppFailure.mapInternal {
                s"Fail to put PKS $pks of account ${ap.account} with key info $PublicKeySummary.Info"
              })
          }
          txResult = Some(
            Transaction.AccountTx.AddPublicKeySummariesResult(toRemove.toMap),
          )
        yield TransactionWithResult(Signed(sig, ap))(txResult)

  def verifySignature[F[_]: Monad: PlayNommState](
      sig: AccountSignature,
      tx: Transaction,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, AccountData] =
    for
      pks <- getPKS(sig, tx)
      keyInfoOption <- PlayNommState[F].account.key
        .get((sig.account, pks))
        .mapK(PlayNommDAppFailure.mapInternal {
          s"Fail to decode key info of ${(sig.account, pks)}"
        })
      keyInfo <- fromOption(
        keyInfoOption,
        s"There is no public key summary $pks from account ${sig.account}",
      )
      accountInfoOption <- getAccountInfo(sig.account)
      accountInfo <- fromOption(
        accountInfoOption,
        s"${sig.account} does not exists",
      )
    yield accountInfo

  def getAccountInfo[F[_]: Monad: PlayNommState](
      account: Account,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Option[
    AccountData,
  ]] = PlayNommState[F].account.name
    .get(account)
    .mapK(PlayNommDAppFailure.mapInternal {
      s"Fail to decode account ${account}"
    })

  def getPKS[F[_]: Monad: PlayNommState](
      sig: AccountSignature,
      tx: Transaction,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    PublicKeySummary,
  ] =
    for pubKey <- fromOption(
        tx.toHash.recover(sig.sig),
        s"Fail to recover public key from $tx",
      )
    yield PublicKeySummary.fromPublicKeyHash(pubKey.toHash)
