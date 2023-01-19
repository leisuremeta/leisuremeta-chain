package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT
import scala.concurrent.ExecutionContext
import io.leisuremeta.chain.lmscan.backend.model.AccountDetail
import io.leisuremeta.chain.lmscan.backend.repository.AccountRepository

object AccountService:
  def get[F[_]: Async](
      address: String,
  )(using ExecutionContext): EitherT[F, String, Option[AccountDetail]] =
    AccountRepository.get(address)
