package io.leisuremeta.chain.lmscan.backend.service

// import io.leisuremeta.chain.lmscan.backend.entity.Summary
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository
import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.repository.BalanceRepository

object SummaryService:
  def get[F[_]: Async]: EitherT[F, String, Option[SummaryModel]] =
    for
      summary <- SummaryRepository.get
      model = summary.map(s =>
        SummaryModel(
          Some(s.id),
          Some(s.lmPrice),
          Some(s.blockNumber),
          Some(s.totalAccounts),
          Some(s.createdAt),
          Some(s.totalTxSize),
          Some(BalanceRepository.getBalance),
        ),
      )
    yield model
