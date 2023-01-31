package io.leisuremeta.chain.lmscan.agent.service

import cats.data.EitherT
import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.repository.BlockRepository
import io.leisuremeta.chain.lmscan.agent.entity.Block


object BlockService:
  def get[F[_]: Async](
    hash: String,
  ): EitherT[F, String, Option[Block]] =
    BlockRepository.get(hash)
