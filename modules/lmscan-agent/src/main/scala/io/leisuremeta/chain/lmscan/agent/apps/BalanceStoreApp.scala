package io.leisuremeta.chain.lmscan.agent
package apps

import cats.effect.*
import cats.effect.kernel.instances.all.*
import cats.implicits.*
import io.leisuremeta.chain.lmscan.agent.service.*
import io.leisuremeta.chain.lib.crypto.Hash
import cats.effect.std.Queue
import io.leisuremeta.chain.api.model.Transaction
import io.leisuremeta.chain.api.model.Transaction.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*
import io.leisuremeta.chain.api.model.Signed.TxHash
import io.leisuremeta.chain.api.model.TransactionWithResult
import scala.concurrent.duration.DurationInt
import cats.data.NonEmptyList

trait BalanceApp[F[_]]:
  def run: F[Unit]

case class Balance(
    address: String,
    free: BigDecimal,
)
case class Ledger(
    hash: String,
    address: String,
    free: Option[String],
    used: Option[String],
)
enum LedgerType:
  case InputLedger(
      hash: String,
      address: String,
      free: BigInt,
  )
  case SpendLedger(
      hash: String,
      address: String,
      used: String,
  )
  case LockLedger(
      hash: String,
      address: String,
      locked: BigInt,
  )
  case SpendLockLedger(
      hash: String,
      used: String,
  )
object BalanceApp:
  def build[F[_]: Async](
      balQ: Queue[F, (TxHash, TransactionWithResult)],
  )(remote: RemoteStoreApp[F], local: LocalStoreApp[F]): BalanceApp[F] =
    new BalanceApp[F]:
      def run =
        for
          _ <- init
          _ <- loop
        yield ()
      def init: F[Unit] =
        for
          _ <- local.balRepo.createLedgerTable
          _ <- local.balRepo.createLockLedgerTable
        yield ()
      def loop: F[Unit] =
        for
          x      <- balQ.tryTakeN(Some(100))
          balOps <- x.flatMap(toBalanceOp).pure[F]
          (a, b, c, d) = balOps.foldLeft(
            (
              List.empty[LedgerType.InputLedger],
              List.empty[LedgerType.SpendLedger],
              List.empty[LedgerType.LockLedger],
              List.empty[LedgerType.SpendLockLedger],
            ),
          )((acc, v) =>
            v match
              case i: LedgerType.InputLedger =>
                (i :: acc._1, acc._2, acc._3, acc._4)
              case s: LedgerType.SpendLedger =>
                (acc._1, s :: acc._2, acc._3, acc._4)
              case l: LedgerType.LockLedger =>
                (acc._1, acc._2, l :: acc._3, acc._4)
              case r: LedgerType.SpendLockLedger =>
                (acc._1, acc._2, acc._3, r :: acc._4),
          )
          _ <- local.balRepo.addInputLedger(a)
          _ <- local.balRepo.addSpendLedger(b)
          _ <- local.balRepo.addLockLedger(c)
          _ <- local.balRepo.addSpendLockLedger(d)
          updates = balOps.foldLeft(
            Set.empty[String],
          )((acc, v) =>
            v match
              case LedgerType.InputLedger(_, address, free) =>
                acc + address
              case LedgerType.SpendLedger(_, address, _) =>
                acc + address
              case _ => acc,
          )
          freeArr <-
            if updates.isEmpty then Async[F].pure(Right(Nil))
            else
              local.balRepo
                .getLedger(
                  NonEmptyList.fromListUnsafe(updates.toList),
                )
          balMap = freeArr match
            case Left(e) => Map.empty[String, BigDecimal]
            case Right(v) =>
              v
                .foldLeft(Map.empty[String, BigDecimal])((acc, b) =>
                  acc.get(b._1) match
                    case Some(v) =>
                      acc + (b._1 -> (v + BigDecimal(b._2)))
                    case None =>
                      acc + (b._1 -> BigDecimal(b._2)),
                )
          _ <-
            if balMap.isEmpty then Async[F].pure(Right(0))
            else remote.balRepo.updateBalance(balMap.toList)
          _ <- Async[F].delay:
            scribe.info("balance updated: " + balMap.size)
          _ <- Async[F].sleep(20.seconds)
          r <- run
        yield r

      def toBalanceOp(
          hash: TxHash,
          tx: TransactionWithResult,
      ) =
        val signer = tx.signedTx.sig.account
        tx.signedTx.value match
          case t: MintFungibleToken =>
            t.outputs
              .map((acc, nat) =>
                LedgerType.InputLedger(
                  hash.toUInt256Bytes.toHex,
                  acc.toString,
                  nat.toBigInt,
                ),
              )
              .toList
          case t: BurnFungibleToken =>
            val inputs = t.inputs
              .map(h =>
                LedgerType.SpendLedger(
                  h.toUInt256Bytes.toHex,
                  signer.toString,
                  hash.toUInt256Bytes.toHex,
                ),
              )
              .toList
            tx.result match
              case Some(BurnFungibleTokenResult(v)) =>
                LedgerType.InputLedger(
                  hash.toUInt256Bytes.toHex,
                  signer.toString,
                  v.toBigInt,
                ) ::
                  inputs
              case _ => inputs
          case t: TransferFungibleToken =>
            t.outputs
              .map((acc, nat) =>
                LedgerType.InputLedger(
                  hash.toUInt256Bytes.toHex,
                  acc.toString,
                  nat.toBigInt,
                ),
              )
              .toList :::
              t.inputs
                .map(h =>
                  LedgerType.SpendLedger(
                    h.toUInt256Bytes.toHex,
                    signer.toString,
                    hash.toUInt256Bytes.toHex,
                  ),
                )
                .toList
          case t: OfferReward =>
            t.outputs
              .map((acc, nat) =>
                LedgerType.InputLedger(
                  hash.toUInt256Bytes.toHex,
                  acc.toString,
                  nat.toBigInt,
                ),
              )
              .toList :::
              t.inputs
                .map(h =>
                  LedgerType.SpendLedger(
                    h.toUInt256Bytes.toHex,
                    signer.toString,
                    hash.toUInt256Bytes.toHex,
                  ),
                )
                .toList
          case t: EntrustFungibleToken =>
            val l = LedgerType.LockLedger(
              hash.toUInt256Bytes.toHex,
              t.to.toString,
              t.amount.toBigInt,
            ) ::
              t.inputs
                .map(h =>
                  LedgerType.SpendLedger(
                    h.toUInt256Bytes.toHex,
                    signer.toString,
                    hash.toUInt256Bytes.toHex,
                  ),
                )
                .toList
            tx.result match
              case Some(EntrustFungibleTokenResult(v)) =>
                LedgerType.InputLedger(
                  hash.toUInt256Bytes.toHex,
                  signer.toString,
                  v.toBigInt,
                ) :: l
              case _ => l
          case t: DisposeEntrustedFungibleToken =>
            t.inputs
              .map(h =>
                LedgerType.SpendLockLedger(
                  h.toUInt256Bytes.toHex,
                  hash.toUInt256Bytes.toHex,
                ),
              )
              .toList :::
              t.outputs
                .map((acc, nat) =>
                  LedgerType.InputLedger(
                    hash.toUInt256Bytes.toHex,
                    acc.toString,
                    nat.toBigInt,
                  ),
                )
                .toList
          case _ => Nil
