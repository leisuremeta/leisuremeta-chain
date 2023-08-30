package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.entity.Summary
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*

object SummaryRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async](n: Int = 0): EitherT[F, String, Option[Summary]] =
    inline def detailQuery =
      quote {
        query[Summary].sortBy(t => t.createdAt)(Ord.desc).drop(lift(n)).take(1)
      }
    optionQuery(detailQuery)

  def getDay[F[_]: Async]: EitherT[F, String, Option[Seq[Summary]]] =
    inline def detailQuery =
      quote {
        query[Summary].sortBy(t => t.createdAt)(Ord.desc).take(144)
      }
    optionSeqQuery(detailQuery)
