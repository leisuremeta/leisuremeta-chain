package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.Account
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*

object AccountRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async](
      address: String,
  ): EitherT[F, String, Option[Account]] =
    inline def detailQuery =
      quote { (addr: String) =>
        query[Account].filter(a => a.address == addr).take(1)
      }
    optionQuery(detailQuery(lift(address)))
