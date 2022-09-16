package io.leisuremeta.chain
package node
package service

import java.time.Instant

import cats.Monad
import cats.data.EitherT

import api.model.{Block, StateRoot}
import api.model.Block.ops.*
import lib.crypto.Hash
import lib.crypto.Hash.ops.*
import lib.datatype.{BigNat, UInt256}
import lib.failure.DecodingFailure
import repository.BlockRepository

object NodeInitializationService:
  def initialize[F[_]: Monad: BlockRepository](
      timestamp: Instant,
  ): EitherT[F, String, Block] = for
    _ <- EitherT.rightT[F, String](scribe.info(s"Initialize... "))
    bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    block <- bestBlockHeaderOption match
      case None =>
        scribe.info(
          "No best block header found. Initializing genesis block.",
        )
        val block = genesisBlock(timestamp)
        BlockRepository[F].put(genesisBlock(timestamp)).leftMap(_.msg).map(_ => block)
      case Some(header) =>
        scribe.info("Best block header found. Skipping genesis block.")
        val blockHash = header.toHash.toBlockHash
        for
          blockOption <- BlockRepository[F].get(blockHash).leftMap(_.msg)
          block <- EitherT.fromOption[F](
            blockOption,
            s"best block $blockHash not found in the block repository",
          )
        yield block
  yield block

  def genesisBlock(genesisTimestamp: Instant): Block = Block(
    header = Block.Header(
      number = BigNat.Zero,
      parentHash = Hash.Value[Block](UInt256.EmptyBytes),
      stateRoot = StateRoot.empty,
      transactionsRoot = None,
      timestamp = genesisTimestamp,
    ),
    transactionHashes = Set.empty,
    votes = Set.empty,
  )
