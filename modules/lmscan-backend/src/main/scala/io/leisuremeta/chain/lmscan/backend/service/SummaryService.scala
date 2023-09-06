package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.common.model.SummaryChart

object SummaryService:
  def get[F[_]: Async]: EitherT[F, Either[String, String], Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get().leftMap(Left(_))
      model = summary.map(s =>
        SummaryModel(
          Some(s.id),
          Some(s.lmPrice),
          Some(s.blockNumber),
          Some(s.totalAccounts),
          Some(s.createdAt),
          Some(s.totalTxSize.toLong),
          Some(s.total_balance),
        ),
      )
    yield model

  def getBeforeDay[F[_]: Async]: EitherT[F, Either[String, String], Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get(143).leftMap(Left(_))
      model = summary.map(s =>
        SummaryModel(
          Some(s.id),
          Some(s.lmPrice),
          Some(s.blockNumber),
          Some(s.totalAccounts),
          Some(s.createdAt),
          Some(s.totalTxSize.toLong),
          Some(s.total_balance),
        ),
      )
    yield model

  def getList[F[_]: Async]: EitherT[F, Either[String, String], SummaryChart] =
    for
      summary <- SummaryRepository.getDay.leftMap(Left(_))
      model = summary.map(_.map(s =>
        SummaryModel(
          Some(s.id),
          Some(s.lmPrice),
          Some(s.blockNumber),
          Some(s.totalAccounts),
          Some(s.createdAt),
          Some(s.totalTxSize.toLong),
          Some(s.total_balance),
        ),
      ))
      chart = SummaryChart(model.get)
    yield chart
