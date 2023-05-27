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
import api.model.Block.ops.toBlockHash
import api.model.api_model.BlockInfo
import dapp.PlayNommState
import repository.{BlockRepository, TransactionRepository}
import lib.crypto.Hash.ops.*

object BlockService:
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
