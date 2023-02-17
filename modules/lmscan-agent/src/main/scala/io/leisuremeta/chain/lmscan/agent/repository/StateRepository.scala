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

  def getBlockStatesByNotBuildedOrderByNumberAsc[F[_]: Async]: EitherT[F, String, Seq[BlockStateEntity]] =
    inline given SchemaMeta[BlockStateEntity] = schemaMeta[BlockStateEntity]("block_state")  
    inline def q =
      quote { query[BlockStateEntity].filter(!_.isBuild).sortBy(t => t.number)(Ord.asc) }
    seqQuery(q)

  def getBlockStateByNotBuildedOrderByNumberAsc[F[_]: Async]: EitherT[F, String, Option[BlockStateEntity]] =
    inline given SchemaMeta[BlockStateEntity] = schemaMeta[BlockStateEntity]("block_state")  
    inline def q =
      quote { query[BlockStateEntity].filter(!_.isBuild).sortBy(t => t.number)(Ord.asc).take(1) }
    seqQuery(q).map(_.headOption)

  def getBlockStateByNotBuildedOrderByNumberAscLimit[F[_]: Async](limit: Int): EitherT[F, String, Seq[BlockStateEntity]] =
    inline given SchemaMeta[BlockStateEntity] = schemaMeta[BlockStateEntity]("block_state")  
    inline def q = quote { query[BlockStateEntity].filter(!_.isBuild).sortBy(t => t.number)(Ord.asc).take(lift(limit)) }
    seqQuery(q)

  def getTxStatesByBlockOrderByEventTimeAsc[F[_]: Async](blockHash: String): EitherT[F, String, Seq[TxStateEntity]] =
    inline given SchemaMeta[TxStateEntity] = schemaMeta[TxStateEntity]("tx_state")  
    inline def q =
      quote { query[TxStateEntity].filter(b => (b.blockHash == lift(blockHash))).sortBy(t => t.eventTime)(Ord.asc) }
    seqQuery(q)

