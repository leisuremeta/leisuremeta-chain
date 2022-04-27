package org.leisuremeta.lmchain.core
package node
package service

import java.time.Instant

import cats.data.EitherT

import crypto.Signature
import model.{Block, Signed}

trait LocalGossipService[F[_]] {

  def get: F[GossipDomain.LocalGossip]

  def onNewTx(tx: Signed.Tx): EitherT[F, String, Unit]

  def generateNewBlockSuggestion(
      currentTime: Instant
  ): EitherT[F, String, Block.BlockHash]

  def onNewBlockSuggestion(block: Block): EitherT[F, String, Unit]

  def onNewBlockVote(
      blockHash: Block.BlockHash,
      nodeNo: Int,
      sig: Signature,
  ): EitherT[F, String, Unit]

  def setBestConfirmedBlock(
      blockHash: Block.BlockHash,
      block: Block,
  ): EitherT[F, String, Unit]
}

object LocalGossipService {
  def apply[F[_]](implicit lgs: LocalGossipService[F]): LocalGossipService[F] =
    lgs
}
