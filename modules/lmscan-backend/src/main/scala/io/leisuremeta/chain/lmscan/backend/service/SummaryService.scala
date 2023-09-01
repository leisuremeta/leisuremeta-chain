package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT

object SummaryService:
  def get[F[_]: Async]: EitherT[F, String, Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get()
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

  def getBeforeDay[F[_]: Async]: EitherT[F, String, Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get(143)
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

  def getList[F[_]: Async]: EitherT[F, String, Option[Seq[SummaryModel]]] =
    for
      summary <- SummaryRepository.getDay
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
    yield model
