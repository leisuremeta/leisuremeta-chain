package io.leisuremeta.chain.lmscan.agent.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.repository.TxRepository

object TxService:
  def countInLatest24h[F[_]: Async]: EitherT[F, String, Long] =
    TxRepository.countInLatest24h[F]
  def txDataSize[F[_]: Async]: EitherT[F, String, Option[Long]] =
    TxRepository.txDataSize[F]
