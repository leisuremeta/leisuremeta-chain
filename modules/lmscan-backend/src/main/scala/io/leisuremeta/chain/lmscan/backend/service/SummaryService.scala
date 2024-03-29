package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.common.model.SummaryChart
import io.leisuremeta.chain.lmscan.common.model.SummaryBoard

object SummaryService:
  def get[F[_]: Async](n: Int): EitherT[F, Either[String, String], Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get(n, 1).leftMap(Left(_))
      model = summary.map(a =>
        val h = a.headOption
        h match
          case Some(s) =>
            SummaryModel(
              Some(s.id),
              Some(s.lmPrice),
              Some(s.blockNumber),
              Some(s.totalAccounts),
              Some(s.createdAt),
              Some(s.totalTxSize.toLong),
              Some(s.totalBalance),
            )
          case None =>
            SummaryModel(
              None,
              None,
              None,
              None,
              None,
              None,
              None,
            )
      )
    yield model

  def getBoard[F[_]: Async]: EitherT[F, Either[String, String], Option[SummaryBoard]] =
    for 
      todayOpt <- get(0)
      yesterdayOpt <- get(143)
      model = todayOpt.zip(yesterdayOpt).map((today, yesterday) => SummaryBoard(today, yesterday))
    yield model

  def get5List[F[_]: Async]: EitherT[F, Either[String, String], SummaryChart] =
    for
      summary <- SummaryRepository.get(0, 144 * 5 + 1).leftMap(Left(_))
      model = summary.map(
          _.grouped(144).map(_.head).map(s => 
            SummaryModel(
              Some(s.id),
              Some(s.lmPrice),
              Some(s.blockNumber),
              Some(s.totalAccounts),
              Some(s.createdAt),
              Some(s.totalTxSize.toLong),
              Some(s.totalBalance),
            )
          ).toSeq
        )
      chart = SummaryChart(model.get)
    yield chart

  def getList[F[_]: Async]: EitherT[F, Either[String, String], SummaryChart] =
    for
      summary <- SummaryRepository.get(0, 144 * 5).leftMap(Left(_))
      model = summary.map(_.map(s =>
        SummaryModel(
          None,
          Some(s.lmPrice),
          None,
          None,
          Some(s.createdAt),
          None,
          Some(s.totalBalance),
        ),
      ))
      chart = SummaryChart(model.get)
    yield chart
