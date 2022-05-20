package io.leisuremeta.chain
package node
package service

import cats.Functor
import cats.data.EitherT

import api.model.{Block, NetworkId, NodeStatus}
import api.model.Block.ops.*
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.failure.DecodingFailure
import repository.BlockRepository
import java.time.Instant

object LocalStatusService:

  def status[F[_]: Functor: BlockRepository](
      networkId: NetworkId,
      genesisTimestamp: Instant,
  ): EitherT[F, DecodingFailure, NodeStatus] =
    for bestBlockHeader <- BlockRepository[F].bestHeader
    yield
      val gHash = genesisHash(genesisTimestamp)
      NodeStatus(
        networkId = networkId,
        genesisHash = gHash,
        bestHash = bestBlockHeader.fold(gHash)(_.toHash.toBlockHash),
        number = bestBlockHeader.fold(BigNat.Zero)(_.number),
      )

  def genesisHash(genesisTimestamp: Instant): Block.BlockHash =
    NodeInitializationService.genesisBlock(genesisTimestamp).toHash
