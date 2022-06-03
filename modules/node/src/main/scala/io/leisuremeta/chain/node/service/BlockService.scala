package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.eq.*
import cats.syntax.foldable.*
import cats.syntax.traverse.*

import api.model.{Block, Signed, TransactionWithResult}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import lib.crypto.Hash.ops.*

object BlockService:

  def saveBlockWithState[F[_]: Concurrent](
      block: Block,
      txs: Map[Signed.TxHash, Signed.Tx],
  )(using
      blockRepo: BlockRepository[F],
      txRepo: TransactionRepository[F],
      namesStateRepo: StateRepository.AccountState.Name[F],
      keyStateRepo: StateRepository.AccountState.Key[F],
      groupStateRepo: StateRepository.GroupState.Group[F],
      groupAccountStateRepo: StateRepository.GroupState.GroupAccount[F],
  ): EitherT[F, String, Block.BlockHash] = for
    _ <- EitherT.rightT[F, String](scribe.info(s"Saving Block: $block"))
    _ <- EitherT.rightT[F, String](scribe.info(s"Saving txs: $txs"))
    _ <- EitherT
      .cond[F](block.transactionHashes === txs.keySet, (), "Invalid txs")
    parentOption <- blockRepo.get(block.header.parentHash).leftMap(_.msg)
    parent <- EitherT
      .fromOption[F](
        parentOption,
        s"parent is not exist: ${block.header.parentHash}",
      )
    parentState = GossipDomain.MerkleState.from(parent.header)
    txList      = txs.values.toList
    stateAndResultList <- txList
      .foldM[EitherT[
        F,
        String,
        *,
      ], (GossipDomain.MerkleState, List[TransactionWithResult])](
        (parentState, Nil),
      ) { case ((state, acc), tx) =>
        StateService.updateStateWithTx[F](state, tx).map {
          case (newState, result) =>
            (newState, result :: acc)
        }
      }
    state      = stateAndResultList._1
    resultList = stateAndResultList._2.reverse
    _ <- EitherT.cond[F](
      state.toStateRoot === block.header.stateRoot,
      (),
      s"State is not correct in block: ${block.header}",
    )
    _ <- EitherT.right[String](
      Monad[F].tuple5(
        resultList.traverse(txRepo.put),
        namesStateRepo.put(state.account.namesState),
        keyStateRepo.put(state.account.keyState),
        groupStateRepo.put(state.group.groupState),
        groupAccountStateRepo.put(state.group.groupAccountState),
      ),
    )
    _ <- saveBlock[F](block, (txs.keys zip resultList).toMap)
  yield
    scribe.info(s"block saved with states: $block")
    block.toHash

  def saveBlock[F[_]: Monad: BlockRepository: TransactionRepository](
      block: Block,
      txs: Map[Signed.TxHash, TransactionWithResult],
  ): EitherT[F, String, Block.BlockHash] = for
    _ <- BlockRepository[F].put(block).leftMap(_.msg)
    _ <- EitherT.rightT[F, String](scribe.info(s"Saving txs: $txs"))
    _ <- block.transactionHashes.toList.traverse { (txHash: Signed.TxHash) =>
      for
        tx <- EitherT
          .fromOption[F](txs.get(txHash), s"Missing transaction: $txHash")
        _ <- EitherT.right[String](TransactionRepository[F].put(tx))
      yield ()
    }
    _ <- EitherT.rightT[F, String](scribe.info(s"txs is saved successfully"))
  yield block.toHash

  def get[F[_]: Functor: BlockRepository](
      blockHash: Block.BlockHash,
  ): EitherT[F, String, Option[Block]] =
    BlockRepository[F].get(blockHash).leftMap(_.msg)
