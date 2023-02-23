package io.leisuremeta.chain.lmscan.agent.repository



import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{SummaryEntity}
import io.getquill.*
import CommonQuery.*

object SummaryRepository:

  import ctx.{*, given}

  def getLastSavedLmPrice[F[_]: Async]: EitherT[F, String, Option[Double]] =
    inline given SchemaMeta[SummaryEntity] = schemaMeta[SummaryEntity]("summary")
    inline def q =
      quote { query[SummaryEntity].sortBy(t => t.createdAt)(Ord.asc) }
    
    for 
      value <- optionQuery(q)
      res <- value match 
        case Some(value) => EitherT.pure(Some(value.lmPrice))
        case None => EitherT.pure(Some(0.0))
    yield res
