package io.leisuremeta.chain.lmscan.agent.service

import cats.data.EitherT
import cats.effect.Async
import io.leisuremeta.chain.lmscan.agent.repository.SummaryRepository

object SummaryService:
  def getLastSavedLmPrice[F[_]: Async]: EitherT[F, String, Option[Double]] =
    SummaryRepository.getLastSavedLmPrice[F]
    
