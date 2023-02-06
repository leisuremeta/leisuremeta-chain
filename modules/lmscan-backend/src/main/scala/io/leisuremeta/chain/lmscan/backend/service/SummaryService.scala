package io.leisuremeta.chain.lmscan.backend.service


import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.entity.Summary
import io.leisuremeta.chain.lmscan.backend.repository.{
  SummaryRepository,
}
import io.leisuremeta.chain.lmscan.backend.model.{PageNavigation, PageResponse}
import cats.implicits.*
import cats.effect.IO

object SummaryService:

  def get[F[_]: Async]: EitherT[F, String, Option[Summary]] =
    SummaryRepository.get(hash)

