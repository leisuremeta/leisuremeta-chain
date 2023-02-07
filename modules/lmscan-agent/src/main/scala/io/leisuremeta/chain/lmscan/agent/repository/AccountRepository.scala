package io.leisuremeta.chain.lmscan.agent.repository

import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery
import io.leisuremeta.chain.lmscan.agent.entity.AccountEntity
import io.getquill.*
import cats.effect.kernel.Async
import cats.data.EitherT

object AccountRepository extends CommonQuery:
  import ctx.{*, given}

  def totalCount[F[_]: Async]: EitherT[F, String, Long] =
    inline def totalQuery = quote {
      query[AccountEntity]
    }
    countQuery(totalQuery)
