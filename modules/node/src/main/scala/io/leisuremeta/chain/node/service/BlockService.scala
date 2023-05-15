package io.leisuremeta.chain
package node
package service

import cats.{Functor, Monad}
import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.eq.*
import cats.syntax.foldable.*
import cats.syntax.functor.*
import cats.syntax.traverse.*

import api.model.{Block, Signed, TransactionWithResult}
import api.model.api_model.BlockInfo
import dapp.PlayNommState
import repository.{BlockRepository, GenericStateRepository, TransactionRepository}
import lib.crypto.Hash.ops.*
import io.leisuremeta.chain.api.model.Block.ops.toBlockHash

object BlockService:

  def saveBlockWithState[F[_]: Concurrent: GenericStateRepository.AccountState: GenericStateRepository.GroupState: GenericStateRepository.TokenState: GenericStateRepository.RewardState: PlayNommState](
      block: Block,
      txs: Map[Signed.TxHash, Signed.Tx],
  )(using
      blockRepo: BlockRepository[F],
      txRepo: TransactionRepository[F],
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
      List(
        resultList.traverse(txRepo.put).map(_ => ()),
//        GenericStateRepository.AccountState[F].name.put(state.account.namesState),
//        GenericStateRepository.AccountState[F].key.put(state.account.keyState),
//        GenericStateRepository.AccountState[F].eth.put(state.account.ethState),
        GenericStateRepository.GroupState[F].group.put(state.group.groupState),
        GenericStateRepository.GroupState[F].groupAccount.put(state.group.groupAccountState),
        GenericStateRepository.TokenState[F].definition.put(state.token.tokenDefinitionState),
        GenericStateRepository.TokenState[F].fungibleBalance.put(state.token.fungibleBalanceState),
        GenericStateRepository.TokenState[F].nftBalance.put(state.token.nftBalanceState),
        GenericStateRepository.TokenState[F].nft.put(state.token.nftState),
        GenericStateRepository.TokenState[F].rarity.put(state.token.rarityState),
        GenericStateRepository.TokenState[F].entrustFungibleBalance.put(state.token.entrustFungibleBalanceState),
        GenericStateRepository.TokenState[F].entrustNftBalance.put(state.token.entrustNftBalanceState),
        GenericStateRepository.RewardState[F].daoState.put(state.reward.daoState),
        GenericStateRepository.RewardState[F].userActivityState.put(state.reward.userActivityState),
        GenericStateRepository.RewardState[F].tokenReceivedState.put(state.reward.tokenReceivedState),
      ).sequence
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

  def index[F[_]: Monad: BlockRepository](
    fromOption: Option[Block.BlockHash],
    limitOption: Option[Int],
  ): EitherT[F, String, List[BlockInfo]] =

    def loop(from: Block.BlockHash, limit: Int, acc: List[BlockInfo]): EitherT[F, String, List[BlockInfo]] =
      if limit <= 0 then EitherT.pure(acc.reverse) else
        BlockRepository[F].get(from).leftMap(_.msg).flatMap {
          case None => EitherT.leftT(s"block not found: $from")
          case Some(block) =>
            val info: BlockInfo = BlockInfo(
              blockNumber = block.header.number,
              timestamp = block.header.timestamp,
              blockHash = from,
              txCount = block.transactionHashes.size,
            )

            if block.header.number.toBigInt <= BigInt(0) then
              EitherT.pure((info :: acc).reverse)
            else
              loop(block.header.parentHash, limit - 1, info :: acc)
        }

    for
      from <- fromOption match
        case Some(from) => EitherT.pure(from)
        case None => BlockRepository[F].bestHeader.leftMap(_.msg).flatMap{
          case Some(blockHeader) => EitherT.pure(blockHeader.toHash.toBlockHash)
          case None => EitherT.leftT(s"Best header not found")
        }
      result <- loop(from, limitOption.getOrElse(50), Nil)
    yield result
    

  def get[F[_]: Functor: BlockRepository](
      blockHash: Block.BlockHash,
  ): EitherT[F, String, Option[Block]] =
    BlockRepository[F].get(blockHash).leftMap(_.msg)
