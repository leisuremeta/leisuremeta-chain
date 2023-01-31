package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.entity.Tx
import cats.data.EitherT

object TxRepository extends CommonQuery:
  def insert[F[_]: Async](tx: Tx): EitherT[F, String, Boolean] =
    ???
