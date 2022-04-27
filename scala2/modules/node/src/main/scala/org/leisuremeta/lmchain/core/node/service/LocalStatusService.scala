package org.leisuremeta.lmchain.core
package node
package service

import cats.Functor
import cats.data.EitherT

import crypto.Hash.ops._
import datatype.BigNat
import failure.DecodingFailure
import model.{Block, NetworkId, NodeStatus}
import model.Block.ops._
import repository.BlockRepository

object LocalStatusService {

  def status[F[_]: Functor: BlockRepository](
      networkId: NetworkId,
      genesisHash: Block.BlockHash,
  ): EitherT[F, DecodingFailure, NodeStatus] = for {
    bestBlockHeader <- BlockRepository[F].bestHeader
  } yield NodeStatus(
    networkId = networkId,
    genesisHash = genesisHash,
    bestHash = bestBlockHeader.fold(genesisHash)(_.toHash.toBlockHash),
    number = bestBlockHeader.fold(BigNat.Zero)(_.number),
  )
}
