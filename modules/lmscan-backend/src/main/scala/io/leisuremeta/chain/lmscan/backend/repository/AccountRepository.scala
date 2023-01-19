package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.model.AccountDetail
import cats.effect.kernel.Async
import scala.concurrent.ExecutionContext
import cats.data.EitherT

object AccountRepository extends CommonQuery:
  def get[F[_]: Async](
      address: String,
  )(using ExecutionContext): EitherT[F, String, Option[AccountDetail]] =
    ???
