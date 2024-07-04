package io.leisuremeta.chain
package node
package dapp
package submodule

import java.time.temporal.ChronoUnit

import cats.Monad
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
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
import api.model.account.{ExternalChain, ExternalChainAddress}
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
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put account key ${ca.account}"
          accountData = AccountData(
            guardian = ca.guardian,
            externalChainAddresses = ca.ethAddress.fold(Map.empty): ethAddress =>
              Map(ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8)),
            lastChecked = ca.createdAt,
            memo = None,
          )
          _ <- PlayNommState[F].account.name
            .put(ca.account, accountData)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put account ${ca.account}"
          _ <- ca.ethAddress.fold(unit): ethAddress =>
            PlayNommState[F].account.externalChainAddresses
              .put(
                (ExternalChain.ETH, ExternalChainAddress(ethAddress.utf8)),
                ca.account,
              )
              .mapK:
                PlayNommDAppFailure.mapInternal:
                  s"Fail to update eth address ${ca.ethAddress}"
        yield TransactionWithResult(Signed(sig, ca))(None)

      case ca: Transaction.AccountTx.CreateAccountWithExternalChainAddresses =>
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
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put account key ${ca.account}"
          accountData = AccountData(
            guardian = ca.guardian,
            externalChainAddresses = ca.externalChainAddresses,
            lastChecked = ca.createdAt,
            memo = ca.memo,
          )
          _ <- PlayNommState[F].account.name
            .put(ca.account, accountData)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put account ${ca.account}"
          _ <- ca.externalChainAddresses.toSeq.traverse:
            (ExternalChain, ExternalChainAddress) =>
              PlayNommState[F].account.externalChainAddresses
                .put((ExternalChain, ExternalChainAddress), ca.account)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to put public chain address ${((ExternalChain, ExternalChainAddress), ca.account)}"
        yield TransactionWithResult(Signed(sig, ca))(None)

      case ua: Transaction.AccountTx.UpdateAccount =>
        for
          _                 <- verifySignature(sig, ua)
          accountDataOption <- getAccountInfo(ua.account)
          accountData <- fromOption(
            accountDataOption,
            s"${ua.account} does not exists",
          )
          _ <- checkExternal(
            sig.account == ua.account ||
              Some(sig.account) == accountData.guardian,
            s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
          )
          accountData1 = accountData.copy(
            guardian = ua.guardian,
            externalChainAddresses = ua.ethAddress.fold(Map.empty): ethAddress =>
              Map(ExternalChain.ETH -> ExternalChainAddress(ethAddress.utf8)),
            lastChecked = ua.createdAt,
            memo = None,
          )
          _ <- PlayNommState[F].account.name
            .put(ua.account, accountData1)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put account ${ua.account}"
          _ <- ua.ethAddress.fold(unit): ethAddress =>
            PlayNommState[F].account.externalChainAddresses
              .put(
                (ExternalChain.ETH, ExternalChainAddress(ethAddress.utf8)),
                ua.account,
              )
              .mapK:
                PlayNommDAppFailure.mapInternal:
                  s"Fail to update eth address ${ua.ethAddress}"
        yield TransactionWithResult(Signed(sig, ua))(None)

      case ua: Transaction.AccountTx.UpdateAccountWithExternalChainAddresses =>
        for
          _                 <- verifySignature(sig, ua)
          accountDataOption <- getAccountInfo(ua.account)
          accountData <- fromOption(
            accountDataOption,
            s"${ua.account} does not exists",
          )
          _ <- checkExternal(
            sig.account == ua.account ||
              Some(sig.account) == accountData.guardian,
            s"Signer ${sig.account} is neither ${ua.account} nor its guardian",
          )
          accountData1 = accountData.copy(
            guardian = ua.guardian,
            externalChainAddresses = ua.externalChainAddresses,
            lastChecked = ua.createdAt,
            memo = ua.memo,
          )
          _ <- PlayNommState[F].account.name
            .put(ua.account, accountData1)
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to put account ${ua.account}"
          _ <- ua.externalChainAddresses.toSeq.traverse:
            (ExternalChain, ExternalChainAddress) =>
              PlayNommState[F].account.externalChainAddresses
                .put((ExternalChain, ExternalChainAddress), ua.account)
                .mapK:
                  PlayNommDAppFailure.mapInternal:
                    s"Fail to update public chain address ${((ExternalChain, ExternalChainAddress), ua.account)}"
        yield TransactionWithResult(Signed(sig, ua))(None)

      case ap: Transaction.AccountTx.AddPublicKeySummaries =>
        for
          _                 <- verifySignature(sig, ap)
          accountDataOption <- getAccountInfo(ap.account)
          accountData <- fromOption(
            accountDataOption,
            s"${ap.account} does not exists",
          )
          _ <- checkExternal(
            sig.account == ap.account ||
              Some(sig.account) == accountData.guardian,
            s"Signer ${sig.account} is neither ${ap.account} nor its guardian ${accountData.guardian}",
          )
          timeToCheck = accountData.lastChecked
            .plus(10, ChronoUnit.DAYS)
            .compareTo(ap.createdAt) < 0
//          toRemove <-
//            if !timeToCheck then pure(Vector.empty)
//            else
//              PlayNommState[F].account.key
//                .from(ap.account.toBytes)
//                .flatMapF { stream =>
//                  stream
//                    .filter { case (_, info) =>
//                      info.expiresAt match
//                        case None => false
//                        case Some(time) =>
//                          time
//                            .plus(40, ChronoUnit.DAYS)
//                            .compareTo(ap.createdAt) < 0
//                    }
//                    .map { case ((account, pks), info) =>
//                      pks -> info.description
//                    }
//                    .compile
//                    .toVector
//                }
//                .mapK(PlayNommDAppFailure.mapInternal {
//                  s"Fail to get PKSes of account ${ap.account}"
//                })
//          _ <- toRemove.traverse { case (pks, _) =>
//            PlayNommState[F].account.key
//              .remove((ap.account, pks))
//              .mapK(PlayNommDAppFailure.mapInternal {
//                s"Fail to remove old PKS $pks from account ${ap.account}"
//              })
//          }
          _ <- ap.summaries.toSeq.traverse: (pks, description) =>
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
              .mapK:
                PlayNommDAppFailure.mapInternal:
                  s"Fail to put PKS $pks of account ${ap.account} with key info $PublicKeySummary.Info"
          txResult = Some:
            Transaction.AccountTx.AddPublicKeySummariesResult(Map.empty)
        yield TransactionWithResult(Signed(sig, ap))(txResult)

  def verifySignature[F[_]: Concurrent: PlayNommState](
      sig: AccountSignature,
      tx: Transaction,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit] =
    for
      pks <- getPKS(sig, tx)
      keyInfoOption <- PlayNommState[F].account.key
        .get((sig.account, pks))
        .mapK:
          PlayNommDAppFailure.mapInternal:
            s"Fail to decode key info of ${(sig.account, pks)}"
      //     _ <- PlayNommState[F].account.key
      //         .from(sig.account.toBytes)
      //         .flatMapF: stream =>
      //           stream.compile.toList.map: list =>
      //             scribe.info(s"===> PKS: $list")
      //         .mapK:
      //           PlayNommDAppFailure.mapInternal:
      //             s"Fail to get stream of PKSes of account ${sig.account}"
      keyInfo <- fromOption(
        keyInfoOption,
        s"There is no public key summary $pks from account ${sig.account}",
      )
      newExpiresAt = keyInfo.expiresAt.map: instant =>
        Seq(instant, tx.createdAt.plus(30, ChronoUnit.DAYS))
          .maxBy(_.toEpochMilli())
      _ <-
        if keyInfo.expiresAt.map(_.toEpochMilli()) ===
            newExpiresAt.map(_.toEpochMilli())
        then
          StateT
            .pure[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Unit](())
        else
          PlayNommState[F].account.key
            .put((sig.account, pks), keyInfo.copy(expiresAt = newExpiresAt))
            .mapK:
              PlayNommDAppFailure.mapInternal:
                s"Fail to update key info of ${(sig.account, pks)} with $keyInfo"
    yield ()

  def getAccountInfo[F[_]: Monad: PlayNommState](
      account: Account,
  ): StateT[EitherT[F, PlayNommDAppFailure, *], MerkleTrieState, Option[
    AccountData,
  ]] = PlayNommState[F].account.name
    .get(account)
    .mapK:
      PlayNommDAppFailure.mapInternal:
        s"Fail to decode account ${account}"

  def getPKS[F[_]: Monad: PlayNommState](
      sig: AccountSignature,
      tx: Transaction,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    PublicKeySummary,
  ] = fromOption(
    tx.toHash.recover(sig.sig),
    s"Fail to recover public key from $tx",
  ).map: pubKey =>
    PublicKeySummary.fromPublicKeyHash(pubKey.toHash)
