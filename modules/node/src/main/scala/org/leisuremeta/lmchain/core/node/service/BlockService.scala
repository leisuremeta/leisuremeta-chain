package org.leisuremeta.lmchain.core
package node
package service

import java.time.Instant

import scala.concurrent.duration.MILLISECONDS

import cats.{Id, Monad}
import cats.data.{EitherT, Kleisli}
import cats.effect.Timer
import cats.implicits._

import crypto.Hash.ops._
import crypto.MerkleTrie
import crypto.MerkleTrie.{MerkleRoot, MerkleTrieState, NodeStore}
import datatype.BigNat
import model._
import model.Block.ops._
import repository.{BlockRepository, StateRepository, TransactionRepository}

object BlockService {

  def get[F[_]: Monad: BlockRepository: LocalGossipService](
      blockHash: Block.BlockHash
  ): EitherT[F, String, Option[Block]] = EitherT
    .right[String](
      LocalGossipService[F].get
        .map(_.newBlockSuggestions.get(blockHash).map(_._1))
    )
    .flatMap {
      case None        => BlockRepository[F].get(blockHash).leftMap(_.msg)
      case Some(block) => EitherT.pure(Some(block))
    }

  def submitTx[F[_]
    : Monad: Timer: TransactionRepository: StateRepository.Name: StateRepository.Token: StateRepository.Balance](
      signedTx: Signed.Tx
  )(implicit
      blockRepo: BlockRepository[F]
  ): EitherT[F, String, Signed.TxHash] = for {
    bestHeaderOption <- blockRepo.bestHeader.leftMap(_.msg)
    bestHeader <- EitherT
      .fromOption[F](bestHeaderOption, "Empty Best Header Option")
    state = StateService.MerkleState.from(bestHeader)
    newState <- StateService.updateStateWithTx[F](state, signedTx)
    txHash = signedTx.toHash
    transactionsRoot <- EitherT.fromEither[F](getTxRoot(txHash))
    now <- EitherT.right[String](Timer[F].clock.realTime(MILLISECONDS))
    newBlockHeader = Block.Header(
      number = BigNat.add(bestHeader.number, BigNat.One),
      parentHash = bestHeader.toHash.toBlockHash,
      namesRoot = newState.namesState.root,
      tokenRoot = newState.tokenState.root,
      balanceRoot = newState.balanceState.root,
      transactionsRoot = Some(transactionsRoot),
      timestamp = Instant.ofEpochMilli(now),
    )
    newBlock = Block(
      header = newBlockHeader,
      transactionHashes = Set(txHash),
      votes = Set.empty,
    )
    _ <- EitherT.right[String](putStateAndTx[F](newState, signedTx))
    _ <- blockRepo.put(newBlock).leftMap(_.msg)
  } yield {
    println(s"tx: $signedTx")
    println(s"tx hash: $txHash")
    println(s"best header: $bestHeader")
    println(s"state: $state")
    println(s"new state: $newState")
    txHash
  }

  def saveBlock[F[_]: Monad: BlockRepository: TransactionRepository](
      block: Block,
      txs: Map[Signed.TxHash, Signed.Tx],
  ): EitherT[F, String, Block.BlockHash] = for {
    _ <- BlockRepository[F].put(block).leftMap(_.msg)
    _ <- block.transactionHashes.toList.traverse { (txHash: Signed.TxHash) =>
      for {
        tx <- EitherT
          .fromOption[F](txs.get(txHash), s"Missing transaction: $txHash")
        _ <- EitherT.right[String](TransactionRepository[F].put(tx))
      } yield ()
    }
  } yield block.toHash

  def saveBlockWithState[F[_]: Monad](
      block: Block,
      txs: Map[Signed.TxHash, Signed.Tx],
  )(implicit
      blockRepo: BlockRepository[F],
      txRepo: TransactionRepository[F],
      namesStateRepo: StateRepository.Name[F],
      tokenStateRepo: StateRepository.Token[F],
      balanceStateRepo: StateRepository.Balance[F],
  ): EitherT[F, String, Block.BlockHash] = for {
    _ <- EitherT
      .cond[F](block.transactionHashes === txs.keySet, (), "Invalid txs")
    parentOption <- blockRepo.get(block.header.parentHash).leftMap(_.msg)
    parent <- EitherT
      .fromOption[F](
        parentOption,
        s"parent is not exist: ${block.header.parentHash}",
      )
    parentState = StateService.MerkleState.from(parent.header)
    txList      = txs.values.toList
    state <- txList.foldM(parentState)(StateService.updateStateWithTx[F])
    isStateCorrect: Boolean = {
      state.namesState.root == block.header.namesRoot &&
      state.tokenState.root == block.header.tokenRoot &&
      state.balanceState.root == block.header.balanceRoot
    }
    _ <- EitherT.cond[F](
      isStateCorrect,
      (),
      s"State is not correct in block: ${block.header}",
    )
    _ <- EitherT.right[String](
      Monad[F].tuple4(
        txList.traverse(txRepo.put),
        namesStateRepo.put(state.namesState),
        tokenStateRepo.put(state.tokenState),
        balanceStateRepo.put(state.balanceState),
      )
    )
    _ <- saveBlock[F](block, txs)
  } yield {
    scribe.debug(s"block saved with states: $block")
    block.toHash
  }

  def putStateAndTx[F[_]: Monad](
      state: StateService.MerkleState,
      tx: Signed.Tx,
  )(implicit
      txRepo: TransactionRepository[F],
      namesStateRepo: StateRepository.Name[F],
      tokenStateRepo: StateRepository.Token[F],
      balanceStateRepo: StateRepository.Balance[F],
  ): F[Unit] = for {
    _ <- namesStateRepo.put(state.namesState)
    _ <- tokenStateRepo.put(state.tokenState)
    _ <- balanceStateRepo.put(state.balanceState)
    _ <- txRepo.put(tx)
  } yield ()

  def getTxRoot(
      txHash: Signed.TxHash
  ): Either[String, MerkleRoot[Signed.TxHash, Unit]] = {

    implicit val idNodeStore: NodeStore[Id, Signed.TxHash, Unit] =
      Kleisli.pure(None)

    val state: Either[String, MerkleTrieState[Signed.TxHash, Unit]] =
      MerkleTrie
        .put[Id, Signed.TxHash, Unit](txHash.bits, ())
        .runS(MerkleTrieState.empty[Signed.TxHash, Unit])
        .value

    state.map(_.root.get)
  }
}
