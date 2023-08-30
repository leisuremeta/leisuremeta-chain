package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.Summary
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.getquill.autoQuote

object SummaryRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async]: EitherT[F, String, Option[Summary]] =
    inline def detailQuery =
      quote {
        // sql"""SELECT * FROM summary order by created_at desc limit 1""".as[Query[Summary]]
        query[Summary].sortBy(t => t.createdAt)(Ord.desc).take(1)
      }
    optionQuery(detailQuery)
