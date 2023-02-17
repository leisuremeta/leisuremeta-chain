package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Summary
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT

object SummaryService:
  def get[F[_]: Async]: EitherT[F, String, Option[Summary]] =
    SummaryRepository.get
