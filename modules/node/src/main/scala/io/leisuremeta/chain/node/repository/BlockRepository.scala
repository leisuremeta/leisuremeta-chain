package io.leisuremeta.chain
package node
package repository

import cats.Monad
import cats.data.EitherT
import cats.implicits._

import api.model.{Block, Signed}
import api.model.Block.BlockHash
import lib.crypto.Hash
import lib.crypto.Hash.ops._
import lib.datatype.BigNat
import lib.failure.DecodingFailure
import store.{HashStore, KeyValueStore, SingleValueStore, StoreIndex}

trait BlockRepository[F[_]] {
  def bestHeader: EitherT[F, DecodingFailure, Option[Block.Header]]
  def get(hash: Hash.Value[Block]): EitherT[F, DecodingFailure, Option[Block]]
  def put(block: Block): EitherT[F, DecodingFailure, Unit]

  def listFrom(
      blockNumber: BigNat,
      limit: Int,
  ): EitherT[F, DecodingFailure, List[(BigNat, BlockHash)]]
  def findByTransaction(
      txHash: Signed.TxHash
  ): EitherT[F, DecodingFailure, Option[BlockHash]]
}

object BlockRepository {

  def apply[F[_]: BlockRepository]: BlockRepository[F] = summon

  def fromStores[F[_]: Monad](using
      bestBlockHeaderStore: SingleValueStore[F, Block.Header],
      blockHashStore: HashStore[F, Block],
      blockNumberIndex: StoreIndex[F, BigNat, BlockHash],
      txBlockIndex: KeyValueStore[F, Signed.TxHash, BlockHash],
  ): BlockRepository[F] = new BlockRepository[F] {

    def bestHeader: EitherT[F, DecodingFailure, Option[Block.Header]] =
      bestBlockHeaderStore.get()

    def get(
        blockHash: Hash.Value[Block]
    ): EitherT[F, DecodingFailure, Option[Block]] =
      blockHashStore.get(blockHash)

    def put(block: Block): EitherT[F, DecodingFailure, Unit] = for {
      _ <- EitherT.rightT[F, DecodingFailure](
        scribe.debug(s"Putting block: $block")
      )
      _                <- EitherT.right[DecodingFailure](blockHashStore.put(block))
      _                <- EitherT.rightT[F, DecodingFailure](scribe.debug(s"block is put"))
      bestHeaderOption <- bestHeader
      _ <- EitherT.rightT[F, DecodingFailure](
        scribe.debug(s"best header option: $bestHeaderOption")
      )
      _ <- (bestHeaderOption match {
        case Some(best) if best.number.toBigInt >= block.header.number.toBigInt =>
          EitherT.pure[F, DecodingFailure](())
        case _ =>
          val blockHash = block.toHash
          EitherT.right[DecodingFailure](for {
            _ <- Monad[F].pure(scribe.debug(s"putting best header"))
            _ <- bestBlockHeaderStore.put(block.header)
            _ <- blockNumberIndex.put(block.header.number, blockHash)
            _ <- block.transactionHashes.toList.traverse { txHash =>
              txBlockIndex.put(txHash, blockHash)
            }
            _ <- Monad[F].pure(scribe.debug(s"putting best header is completed"))
          } yield ())
      })
      _ <- EitherT.rightT[F, DecodingFailure](
        scribe.debug(s"Putting completed: $block")
      )
    } yield ()

    def listFrom(
        blockNumber: BigNat,
        limit: Int,
    ): EitherT[F, DecodingFailure, List[(BigNat, BlockHash)]] =
      blockNumberIndex.from(blockNumber, 0, limit)

    def findByTransaction(
        txHash: Signed.TxHash
    ): EitherT[F, DecodingFailure, Option[BlockHash]] =
      txBlockIndex.get(txHash)
  }
}
