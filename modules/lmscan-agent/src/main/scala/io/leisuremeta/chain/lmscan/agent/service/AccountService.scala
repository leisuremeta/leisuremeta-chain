package io.leisuremeta.chain.lmscan.agent.service

import io.leisuremeta.chain.lmscan.agent.repository.AccountRepository
import cats.effect.kernel.Async
import cats.data.EitherT

object AccountService:
  def totalCount[F[_]: Async]: EitherT[F, String, Long] =
    AccountRepository.totalCount[F]
