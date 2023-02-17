package io.leisuremeta.chain.lmscan.agent.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.repository.NftRepository
import io.leisuremeta.chain.lmscan.agent.entity.NftTxEntity

object NftService:
  def getByTxHash[F[_]: Async](
    hash: String,
  ): EitherT[F, String, Option[NftTxEntity]] =
    NftRepository.getByTxHash(hash)
