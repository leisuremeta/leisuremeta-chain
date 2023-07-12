package io.leisuremeta.chain.lmscan.backend.service

// import io.leisuremeta.chain.lmscan.backend.entity.Summary
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.repository.PlaynommBalanceRepository

object TempService:
  def getBalance[F[_]: Async]: EitherT[F, String, Option[String]] =
    for balance <- PlaynommBalanceRepository.getBalance
    yield balance
