package io.leisuremeta.chain
package bulkinsert

import scala.io.Source

import cats.data.{EitherT}
import cats.effect.{IO, Sync}
import cats.syntax.all.*

import fs2.Stream
import io.circe.parser.decode

import api.model.{Account, Signed, Transaction}
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat

final case class FungibleBalanceState(
    free: Map[Account, Map[Signed.TxHash, (Signed.Tx, BigNat)]],
    locked: Map[Account, Map[Signed.TxHash, (Signed.Tx, BigNat, Account)]],
):
  def addFree(
      account: Account,
      tx: Signed.Tx,
      amount: BigNat,
  ): FungibleBalanceState =
    val txHash     = tx.toHash
    val accountMap = free.getOrElse(account, Map.empty)
    val txMap      = accountMap.updated(txHash, (tx, amount))
    copy(free = free.updated(account, txMap))
  def addLocked(
      account: Account,
      tx: Signed.Tx,
      amount: BigNat,
      from: Account,
  ): FungibleBalanceState =
    val txHash     = tx.toHash
    val accountMap = locked.getOrElse(account, Map.empty)
    val txMap      = accountMap.updated(txHash, (tx, amount, from))
    copy(locked = locked.updated(account, txMap))
  def removeFree(
      account: Account,
      txHash: Signed.TxHash,
  ): FungibleBalanceState =
    val accountMap = free.getOrElse(account, Map.empty)
    val txMap      = accountMap.removed(txHash)
    copy(free = free.updated(account, txMap))
  def removeLocked(
      account: Account,
      txHash: Signed.TxHash,
  ): FungibleBalanceState =
    val accountMap = locked.getOrElse(account, Map.empty)
    val txMap      = accountMap.removed(txHash)
    copy(locked = locked.updated(account, txMap))

object FungibleBalanceState:
  def empty: FungibleBalanceState = FungibleBalanceState(Map.empty, Map.empty)

  def build(
      source: Source,
  ): IO[FungibleBalanceState] =
    val indexWithTxsStream = Stream
      .fromIterator[EitherT[IO, String, *]](source.getLines(), 1)
      .evalMap: line =>
        line.split("\t").toList match
          case blockNumber :: txHash :: jsonString :: Nil =>
            EitherT
              .fromEither[IO]:
                decode[Signed.Tx](jsonString)
              .leftMap: e =>
                scribe.error(s"Error decoding line #$blockNumber: $txHash: $jsonString: $e")
                e.getMessage()
              .map(tx => (BigInt(blockNumber).longValue, tx))
          case _ =>
            scribe.error(s"Error parsing line: $line")
            EitherT.leftT[IO, (Long, Signed.Tx)](s"Error parsing line: $line")
      .groupAdjacentBy(_._1)
      .map:
        case (blockNumber, chunk) =>
          (blockNumber, chunk.toList.map(_._2))

    //val indexWithTxsStream = Stream
    //  .fromIterator[EitherT[IO, String, *]](source.getLines(), 1)
    //  .zipWithIndex
    //  .filterNot(_._1 === "[]")
    //  .evalMap: (line, index) =>
    //    EitherT
    //      .fromEither[IO]:
    //        decode[Seq[Signed.Tx]](line)
    //      .leftMap: e =>
    //        scribe.error(s"Error decoding line #$index: $line: $e")
    //        e.getMessage()
    //      .map(txs => (index, txs))

    def logWrongTx(
        from: Account,
        amount: BigInt,
        tx: Signed.Tx,
        inputs: Map[Signed.TxHash, BigNat],
    ): Unit =
      println(s"$from\t$amount\t${tx.value.toHash}\t$tx")
      inputs.foreach { (txHash, amount) => println(s"===> $txHash : $amount") }

    val stateStream = indexWithTxsStream.evalMapAccumulate[EitherT[
      IO,
      String,
      *,
    ], FungibleBalanceState, (Long, Seq[Signed.Tx])](
      FungibleBalanceState.empty,
    ):
      case (balanceState, (index, txs)) =>
//      if index % 10000 === 0 then
//        println(s"Index: $index")
        val finalState = txs.foldLeft(balanceState): (state, tx) =>
          tx.value match
            case fb: Transaction.FungibleBalance =>
              fb match
                case mt: Transaction.TokenTx.MintFungibleToken =>
                  mt.outputs.foldLeft(state):
                    case (state, (to, amount)) =>
                      state.addFree(to, tx, amount)
                case tt: Transaction.TokenTx.TransferFungibleToken =>
                  val inputList = tt.inputs.toList
                  val inputAmounts = inputList.map: inputTxHash =>
                    state.free
                      .get(tx.sig.account)
                      .getOrElse(Map.empty)
                      .get(inputTxHash)
                      .fold {
//                    scribe.error(s"input $inputTxHash is not exist in tx $txHash")
                        BigNat.Zero
                      }(_._2)
                  val inputs     = inputList.zip(inputAmounts).toMap
                  val inputTotal = inputAmounts.fold(BigNat.Zero)(BigNat.add)
                  val outputTotal =
                    tt.outputs.map(_._2).fold(BigNat.Zero)(BigNat.add)
                  val remainder = inputTotal.toBigInt - outputTotal.toBigInt
                  if remainder < 0 then
                    logWrongTx(
                      tx.sig.account,
                      -remainder,
                      tx,
                      inputs,
                    )
                  val afterRemovingInput = tt.inputs.foldLeft(state):
                    case (state, inputTxHash) =>
                      state.removeFree(tx.sig.account, inputTxHash)
                  val afterAddingOutput =
                    tt.outputs.foldLeft(afterRemovingInput):
                      case (state, (to, amount)) =>
                        state.addFree(to, tx, amount)
                  afterAddingOutput
                case bt: Transaction.TokenTx.BurnFungibleToken =>
                  val inputList = bt.inputs.toList
                  val inputAmounts = inputList.map: inputTxHash =>
                    state.free
                      .get(tx.sig.account)
                      .getOrElse(Map.empty)
                      .get(inputTxHash)
                      .fold {
//                    scribe.error(s"input $inputTxHash is not exist in tx $txHash")
                        BigNat.Zero
                      }(_._2)
                  val inputs     = inputList.zip(inputAmounts).toMap
                  val inputTotal = inputAmounts.fold(BigNat.Zero)(BigNat.add)
                  val burnAmount = bt.amount.toBigInt
                  val remainder  = inputTotal.toBigInt - burnAmount
                  if remainder < 0 then
                    logWrongTx(
                      tx.sig.account,
                      -remainder,
                      tx,
                      inputs,
                    )
                  val afterRemovingInput = bt.inputs.foldLeft(state):
                    case (state, inputTxHash) =>
                      state.removeFree(tx.sig.account, inputTxHash)
                  afterRemovingInput.addFree(
                    tx.sig.account,
                    tx,
                    BigNat.unsafeFromBigInt(remainder.max(0)),
                  )
                case et: Transaction.TokenTx.EntrustFungibleToken =>
                  val inputList = et.inputs.toList
                  val inputAmounts = inputList.map: inputTxHash =>
                    state.free
                      .get(tx.sig.account)
                      .getOrElse(Map.empty)
                      .get(inputTxHash)
                      .fold {
//                    scribe.error(s"input $inputTxHash is not exist in tx $txHash")
                        BigNat.Zero
                      }(_._2)
                  val inputs        = inputList.zip(inputAmounts).toMap
                  val inputTotal    = inputAmounts.fold(BigNat.Zero)(BigNat.add)
                  val entrustAmount = et.amount.toBigInt
                  val remainder     = inputTotal.toBigInt - entrustAmount
                  if remainder < 0 then
                    logWrongTx(
                      tx.sig.account,
                      -remainder,
                      tx,
                      inputs,
                    )
                  val afterRemovingInput = et.inputs.foldLeft(state):
                    case (state, inputTxHash) =>
                      state.removeFree(tx.sig.account, inputTxHash)
                  val afterAddingOutput =
                    afterRemovingInput
                      .addLocked(et.to, tx, et.amount, tx.sig.account)
                      .addFree(
                        tx.sig.account,
                        tx,
                        BigNat.unsafeFromBigInt(remainder.max(0)),
                      )
                  afterAddingOutput
                case dt: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
                  val inputList = dt.inputs.toList
                  val inputAmounts = inputList.map: inputTxHash =>
                    state.locked
                      .get(tx.sig.account)
                      .getOrElse(Map.empty)
                      .get(inputTxHash)
                      .fold {
//                    scribe.error(s"input $inputTxHash is not exist in tx $txHash")
                        BigNat.Zero
                      }(_._2)
                  val inputs     = inputList.zip(inputAmounts).toMap
                  val inputTotal = inputAmounts.fold(BigNat.Zero)(BigNat.add)
                  val outputTotal =
                    dt.outputs.map(_._2).fold(BigNat.Zero)(BigNat.add)
                  val remainder = inputTotal.toBigInt - outputTotal.toBigInt
                  if remainder < 0 then
                    logWrongTx(
                      tx.sig.account,
                      -remainder,
                      tx,
                      inputs,
                    )
                  val afterRemovingInput = dt.inputs.foldLeft(state):
                    case (state, inputTxHash) =>
                      state.removeLocked(tx.sig.account, inputTxHash)
                  val afterAddingOutput =
                    dt.outputs.foldLeft(afterRemovingInput):
                      case (state, (to, amount)) =>
                        state.addFree(to, tx, amount)
                  afterAddingOutput
                case or: Transaction.RewardTx.OfferReward =>
                  val inputList = or.inputs.toList
                  val inputAmounts = inputList.map: inputTxHash =>
                    state.free
                      .get(tx.sig.account)
                      .getOrElse(Map.empty)
                      .get(inputTxHash)
                      .fold {
//                    scribe.error(s"input $inputTxHash is not exist in tx $txHash")
                        BigNat.Zero
                      }(_._2)
                  val inputs     = inputList.zip(inputAmounts).toMap
                  val inputTotal = inputAmounts.fold(BigNat.Zero)(BigNat.add)
                  val outputTotal =
                    or.outputs.map(_._2).fold(BigNat.Zero)(BigNat.add)
                  val remainder = inputTotal.toBigInt - outputTotal.toBigInt
                  if remainder < 0 then
                    logWrongTx(
                      tx.sig.account,
                      -remainder,
                      tx,
                      inputs,
                    )
                  val afterRemovingInput = or.inputs.foldLeft(state):
                    case (state, inputTxHash) =>
                      state.removeFree(tx.sig.account, inputTxHash)
                  val afterAddingOutput =
                    or.outputs.foldLeft(afterRemovingInput):
                      case (state, (to, amount)) =>
                        state.addFree(to, tx, amount)
                  afterAddingOutput
                case er: Transaction.RewardTx.ExecuteReward =>
                  ???
                case er: Transaction.RewardTx.ExecuteOwnershipReward =>
                  ???
            case _ => state
        EitherT.pure[IO, String]((finalState, (index, txs)))

    stateStream.last.compile.toList
      .map(_.headOption.flatten.get._1)
      .value
      .map:
        case Left(err) =>
          scribe.error(s"Error building balance map: $err")
          FungibleBalanceState.empty
        case Right(balanceState) =>
          balanceState
