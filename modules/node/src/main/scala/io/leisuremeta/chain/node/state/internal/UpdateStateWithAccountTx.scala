package io.leisuremeta.chain
package node
package state
package internal

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.eq.given
import cats.syntax.foldable.given
import cats.syntax.traverse.given

import scodec.bits.BitVector

import GossipDomain.MerkleState
import UpdateState.*
import api.model.{
  Account,
  AccountSignature,
  PublicKeySummary,
  Signed,
  Transaction,
  TransactionWithResult,
}
import lib.merkle.MerkleTrie
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.datatype.Utf8
import repository.StateRepository
import repository.StateRepository.given

trait UpdateStateWithAccountTx:
  given updateStateWithAccountTx[F[_]: Concurrent: StateRepository.AccountState]
      : UpdateState[F, Transaction.AccountTx] =
    (ms: MerkleState, sig: AccountSignature, tx: Transaction.AccountTx) =>

      def getAccount: EitherT[F, String, Option[Option[Account]]] = MerkleTrie
        .get[F, Account, Option[Account]](tx.account.toBytes.bits)
        .runA(ms.account.namesState)

      def getKeyInfo(
          account: Account,
          publicKeySummary: PublicKeySummary,
      ): EitherT[F, String, Option[PublicKeySummary.Info]] = MerkleTrie
        .get((account, publicKeySummary).toBytes.bits)
        .runA(ms.account.keyState)

      def getKeyList(
          account: Account,
      ): EitherT[F, String, Vector[(PublicKeySummary, PublicKeySummary.Info)]] =
        MerkleTrie
          .from[F, (Account, PublicKeySummary), PublicKeySummary.Info](
            account.toBytes.bits,
          )
          .runA(ms.account.keyState)
          .flatMap { (stream) =>
            stream
              .takeWhile(_._1.startsWith(account.toBytes.bits))
              .compile
              .toVector
              .flatMap { (vector: Vector[(BitVector, PublicKeySummary.Info)]) =>
                vector.traverse { case (bits, info) =>
                  ByteDecoder[(Account, PublicKeySummary)].decode(
                    bits.bytes,
                  ) match
                    case Right(
                          DecodeResult((account, publicKeySummary), remainder),
                        ) =>
                      if remainder.isEmpty then
                        Concurrent[EitherT[F, String, *]]
                          .pure(publicKeySummary -> info)
                      else
                        Concurrent[EitherT[F, String, *]].raiseError(
                          new Exception(
                            s"Remainder not empty in decoding keylist of $account: $remainder",
                          ),
                        )
                    case Left(err) =>
                      Concurrent[EitherT[F, String, *]]
                        .raiseError(err)
                }
              }
          }

      tx match
        case ca: Transaction.AccountTx.CreateAccount =>
          getAccount.flatMap {
            case None =>
              for
                accountState1 <- MerkleTrie
                  .put(sig.account.toBytes.bits, ca.guardian)
                  .runS(ms.account.namesState)
                keyState1 <-
                  if ca.guardian === Some(sig.account) then
                    EitherT.pure[F, String](ms.account.keyState)
                  else
                    for
                      pubKeySummary <- EitherT
                        .fromEither[F](recoverSignature(ca, sig.sig))
                      info = PublicKeySummary.Info(
                        Utf8
                          .unsafeFrom(
                            "Automatically added in account creation",
                          ),
                        ca.createdAt,
                      )
                      keyState <- MerkleTrie
                        .put((ca.account, pubKeySummary).toBytes.bits, info)
                        .runS(ms.account.keyState)
                    yield keyState
              yield (
                ms.copy(account =
                  ms.account
                    .copy(namesState = accountState1, keyState = keyState1),
                ),
                TransactionWithResult(Signed(sig, ca), None),
              )
            case Some(_) => EitherT.leftT("Account already exists")
          }

        case ap: Transaction.AccountTx.AddPublicKeySummaries =>
          getAccount
            .flatMap {
              case None => EitherT.leftT("Account does not exist")
              case Some(Some(guardian)) if sig.account === guardian =>
                for
                  pubKeySummary <- EitherT
                    .fromEither[F](recoverSignature(ap, sig.sig))
                  keyInfoOption <- getKeyInfo(guardian, pubKeySummary)
                  _ <- EitherT.fromOption[F](
                    keyInfoOption,
                    s"Guardian key $pubKeySummary does not exist",
                  )
                  keyList <- getKeyList(ap.account)
                  toRemove: Vector[(PublicKeySummary, PublicKeySummary.Info)] =
                    if keyList.size > 9 then
                      keyList.toVector
                        .sortBy(_._2.addedAt.toEpochMilli)
                        .dropRight(9)
                    else Vector.empty
                  keyState <- {
                    for
                      _ <- toRemove.traverse { case (key, _) =>
                        MerkleTrie
                          .remove[
                            F,
                            (Account, PublicKeySummary),
                            PublicKeySummary.Info,
                          ]((ap.account, key).toBytes.bits)
                      }
                      _ <- ap.summaries.toVector
                        .traverse { case (key, description) =>
                          MerkleTrie
                            .put[
                              F,
                              (Account, PublicKeySummary),
                              PublicKeySummary.Info,
                            ](
                              (ap.account, key).toBytes.bits,
                              PublicKeySummary.Info(description, ap.createdAt),
                            )
                        }
                    yield ()
                  }.runS(ms.account.keyState)
                  result = Transaction.AccountTx.AddPublicKeySummariesResult(
                    toRemove.toMap.view.mapValues(_.description).toMap,
                  )
                  txWithResult =
                    TransactionWithResult(Signed(sig, ap), Some(result))
                yield (
                  ms.copy(account = ms.account.copy(keyState = keyState)),
                  txWithResult,
                )
              case Some(_) if sig.account === ap.account =>
                for
                  pubKeySummary <- EitherT
                    .fromEither[F](recoverSignature(ap, sig.sig))
                  keyInfoOption <- getKeyInfo(ap.account, pubKeySummary)
                  _ <- EitherT.fromOption[F](
                    keyInfoOption,
                    s"Account '${ap.account}' key $pubKeySummary does not exist",
                  )
                  keyList <- getKeyList(ap.account)
                  toRemove: Vector[(PublicKeySummary, PublicKeySummary.Info)] =
                    if keyList.size > 9 then
                      keyList.toVector
                        .sortBy(_._2.addedAt.toEpochMilli)
                        .dropRight(9)
                    else Vector.empty
                  keyState <- {
                    for
                      _ <- toRemove.traverse { case (key, _) =>
                        MerkleTrie
                          .remove[
                            F,
                            (Account, PublicKeySummary),
                            PublicKeySummary.Info,
                          ]((ap.account, key).toBytes.bits)
                      }
                      _ <- ap.summaries.toVector
                        .traverse { case (key, description) =>
                          MerkleTrie
                            .put[
                              F,
                              (Account, PublicKeySummary),
                              PublicKeySummary.Info,
                            ](
                              (ap.account, key).toBytes.bits,
                              PublicKeySummary.Info(description, ap.createdAt),
                            )
                        }
                    yield ()
                  }.runS(ms.account.keyState)
                  result = Transaction.AccountTx.AddPublicKeySummariesResult(
                    toRemove.toMap.view.mapValues(_.description).toMap,
                  )
                  txWithResult =
                    TransactionWithResult(Signed(sig, ap), Some(result))
                yield (
                  ms.copy(account = ms.account.copy(keyState = keyState)),
                  txWithResult,
                )
              case Some(_) =>
                EitherT.leftT(
                  s"Account does not match signature: ${ap.account} vs ${sig.account}",
                )
            }
