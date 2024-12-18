package io.leisuremeta.chain
package bulkinsert

import scala.io.Source

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*

import fs2.Stream
import io.circe.parser.decode

import api.model.*
import api.model.token.*
import lib.crypto.Hash.ops.*
import lib.datatype.Utf8

final case class NftBalanceState(
    free: Map[Account, Map[Signed.TxHash, (Signed.Tx, TokenId)]],
    locked: Map[Account, Map[Signed.TxHash, (Signed.Tx, TokenId, Account)]],
    tokenOwner: Map[TokenId, Account],
):
  def addFree(
      account: Account,
      tx: Signed.Tx,
      tokenId: TokenId,
  ): NftBalanceState =
    val txHash     = tx.toHash
    val accountMap = free.getOrElse(account, Map.empty)
    val txMap      = accountMap.updated(txHash, (tx, tokenId))
    val tokenOwner1 = tokenOwner.updated(tokenId, account)
    copy(free = free.updated(account, txMap), tokenOwner = tokenOwner1)

  def addLocked(
      account: Account,
      tx: Signed.Tx,
      tokenId: TokenId,
      from: Account,
  ): NftBalanceState =
    val txHash     = tx.toHash
    val accountMap = locked.getOrElse(account, Map.empty)
    val txMap      = accountMap.updated(txHash, (tx, tokenId, from))
    copy(locked = locked.updated(account, txMap))

  def removeFree(
      account: Account,
      txHash: Signed.TxHash,
  ): NftBalanceState =
    val accountMap = free.getOrElse(account, Map.empty)
    val txMap      = accountMap.removed(txHash)
    val tokenOwner1 = accountMap.get(txHash)
      .fold:
//        println(s"No locked balance of account $account with $txHash")
        tokenOwner
      .apply: (_, tokenId) =>
        tokenOwner.removed(tokenId)

    copy(free = free.updated(account, txMap), tokenOwner = tokenOwner1)

  def removeLocked(
      account: Account,
      txHash: Signed.TxHash,
  ): NftBalanceState =
    val accountMap = locked.getOrElse(account, Map.empty)
    val txMap      = accountMap.removed(txHash)
    val tokenOwner1 = accountMap.get(txHash)
      .fold:
//        println(s"No locked balance of account $account with $txHash")
        tokenOwner
      .apply: (_, tokenId, _) =>
        tokenOwner.removed(tokenId)

    copy(locked = locked.updated(account, txMap), tokenOwner = tokenOwner1)


object NftBalanceState:
  def empty: NftBalanceState = NftBalanceState(Map.empty, Map.empty, Map.empty)

  def build(
      source: Source,
  ): IO[NftBalanceState] =
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

//    val indexWithTxsStream = Stream
//      .fromIterator[EitherT[IO, String, *]](source.getLines(), 1)
//      .zipWithIndex
//      .filterNot(_._1 === "[]")
//      .evalMap: (line, index) =>
//        EitherT
//          .fromEither[IO]:
//            decode[Seq[Signed.Tx]](line)
//          .leftMap: e =>
//            scribe.error(s"Error decoding line #$index: $line: $e")
//            e.getMessage()
//          .map(txs => (index, txs))

    def logWrongTx(
        from: Account,
        tokenId: TokenId,
        tx: Signed.Tx,
    ): Unit =
      println(s"No free NFT balance of $from with tokenId $tokenId: ${tx.toHash}: $tx")
      ()

    def logWrongEntrustTx(
        from: Account,
        tokenId: TokenId,
        tx: Signed.Tx,
    ): Unit =
      println(s"No entrust NFT balance of $from with tokenId $tokenId: ${tx.toHash}: $tx")
      ()

    val stateStream = indexWithTxsStream.evalMapAccumulate[EitherT[IO, String, *], NftBalanceState, (Long, Seq[Signed.Tx])](NftBalanceState.empty):
      case (balanceState, (index, txs)) =>    
        val finalState = txs.foldLeft(balanceState): (state, tx) =>
          tx.value match
            case mn: Transaction.TokenTx.MintNFT =>
              state.addFree(mn.output, tx, mn.tokenId)
            case bn: Transaction.TokenTx.BurnNFT =>
              state.removeFree(tx.sig.account, bn.input)
            case tn: Transaction.TokenTx.TransferNFT =>
              val inputOption = for
                nftBalance <- state.free.get(tx.sig.account)
                txAndTokenId <- nftBalance.get(tn.input)
              yield txAndTokenId
              inputOption match
                case Some((inputTx, tokenId)) =>
                  state
                    .removeFree(tx.sig.account, tn.input)
                    .addFree(tn.output, tx, tokenId)
                case None =>
                  if tx.sig.account === Account(Utf8.unsafeFrom("playnomm")) then
                    val currentOwnerOption =
                      state.free.toSeq
                        .flatMap: (account, map) =>
                          map.toSeq.map:
                            case (txHash, (tx, tokenId)) => (tokenId, account, txHash)
                        .find(_._1 === tn.tokenId)
                    currentOwnerOption
                      .fold(state): (_, owner, txHash) =>
                        state.removeFree(owner, txHash)
                      .addFree(tn.output, tx, tn.tokenId)
                  else
                    logWrongTx(tx.sig.account, tn.tokenId, tx)
                    state
            case en: Transaction.TokenTx.EntrustNFT =>
              val inputOption = for
                nftBalance <- state.free.get(tx.sig.account)
                txAndTokenId <- nftBalance.find(_._2._2 === en.tokenId).map(_._2)
              yield txAndTokenId
//              if en.tokenId.utf8.value === "2022101110000890000000405" then
//                scribe.info(s"EntrustNFT(${tx.toHash}): ${tx}")
//                scribe.info(s"Balance of signer: ${state.free.get(tx.sig.account)}")
//                scribe.info(s"input: $inputOption")
              inputOption match
                case Some((inputTx, tokenId)) =>
                  state
                    .removeFree(tx.sig.account, en.input)
                    .addLocked(en.to, tx, tokenId, tx.sig.account)
                case None =>
                  logWrongTx(tx.sig.account, en.tokenId, tx)
                  state
            case dn: Transaction.TokenTx.DisposeEntrustedNFT =>
              val inputOption = for
                nftBalance <- state.locked.get(tx.sig.account)
                txTokenIdAndFrom <- nftBalance.get(dn.input)
              yield txTokenIdAndFrom
              inputOption match
                case Some((inputTx, tokenId, from)) =>
                  // Remove the NFT from the locked state
                  state
                    .removeLocked(tx.sig.account, dn.input)
                    // Add the NFT to the free state
                    .addFree(dn.output.getOrElse(from), tx, tokenId)
                case None =>
                  // If the NFT is not in the locked state, check the free state
                  val to = for
                    output <- dn.output
                    nftBalance <- state.free.get(output)
                    txAndTokenId <- nftBalance.find(_._2._2 === dn.tokenId).map(_._2)
                  yield txAndTokenId
                  // If the NFT is not in the free state, check if the transaction is from the game owner
                  if to.isEmpty then
                    if tx.sig.account === Account(Utf8.unsafeFrom("playnomm")) then
                      // If the transaction is from the game owner, add the NFT to the free state
                      val currentOwnerOption =
                        state.free.toSeq
                          .flatMap: (account, map) =>
                            map.toSeq.map:
                              case (txHash, (tx, tokenId)) => (tokenId, account, txHash)
                          .find(_._1 === dn.tokenId)
                      val state1 = currentOwnerOption
                        .fold(state): (_, owner, txHash) =>
                          state.removeFree(owner, txHash)
                      dn.output.fold(state1): output =>
                        state1.addFree(output, tx, dn.tokenId)
                    else
                      // If the transaction is not from the game owner, log an error
                      logWrongEntrustTx(tx.sig.account, dn.tokenId, tx)
                      state
                  else
                    // If the NFT is in the free state, return the existing state
                    state  
            case _ => state

        EitherT.pure[IO, String]((finalState, (index, txs)))

    stateStream.last.compile.toList
      .map(_.headOption.flatten.get._1)
      .value
      .map:
        case Left(err) =>
          scribe.error(s"Error building balance map: $err")
          NftBalanceState.empty
        case Right(balanceState) =>
          balanceState
