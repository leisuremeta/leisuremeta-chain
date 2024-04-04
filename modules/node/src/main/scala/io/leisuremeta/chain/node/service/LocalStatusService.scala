package io.leisuremeta.chain
package node
package service

import cats.MonadError
import cats.syntax.flatMap.*

import api.model.{Block, NetworkId, NodeStatus}
import api.model.Block.ops.*
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.failure.DecodingFailure
import repository.BlockRepository
import java.time.Instant

object LocalStatusService:

  def status[F[_]: BlockRepository](
      networkId: NetworkId,
      genesisTimestamp: Instant,
  )(using me: MonadError[F, Throwable]): F[NodeStatus] =
    BlockRepository[F].bestHeader.value.flatMap {
      case Left(err) => me.raiseError(err)
      case Right(bestBlockHeader) =>
        val gHash = genesisHash(genesisTimestamp)
        me.pure {
          NodeStatus(
            networkId = networkId,
            genesisHash = gHash,
            bestHash = bestBlockHeader.fold(gHash)(_.toHash.toBlockHash),
            number = bestBlockHeader.fold(BigNat.Zero)(_.number),
          )
        }
    }

  def genesisHash(genesisTimestamp: Instant): Block.BlockHash =
    NodeInitializationService.genesisBlock(genesisTimestamp).toHash
