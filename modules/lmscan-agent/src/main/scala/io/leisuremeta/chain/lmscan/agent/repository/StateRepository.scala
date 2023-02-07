package io.leisuremeta.chain.lmscan.agent.repository

import io.leisuremeta.chain.lmscan.agent.entity.{BlockStateEntity, TxStateEntity}
import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery
import cats.effect.kernel.Async
import io.getquill.Query
import io.getquill.*
import cats.data.EitherT
import CommonQuery.*

object StateRepository:

  import ctx.{*, given}

  def getBlockStatesByNotBuildedOrderByEventTimeAsc[F[_]: Async]: EitherT[F, String, Seq[BlockStateEntity]] =
    inline def q =
      quote { query[BlockStateEntity].filter(!_.isBuild).sortBy(t => t.eventTime)(Ord.asc) }
    seqQuery(q)

  def getTxStatesByBlockAndNotBuildedOrderByEventTimeAsc[F[_]: Async](blockHash: String): EitherT[F, String, Seq[TxStateEntity]] =
    inline def q =
      quote { query[TxStateEntity].filter(!_.isBuild).sortBy(t => t.eventTime)(Ord.asc) }
    seqQuery(q)

