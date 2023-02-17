package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{BlockSavedLog, BlockStateEntity, BlockEntity}
import io.getquill.*
import CommonQuery.*

object BlockRepository:

  import ctx.{*, given}

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[BlockEntity]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[BlockEntity].filter(b => b.hash == hash).take(1)
      }
    optionQuery(detailQuery(lift(hash)))

  def getLastSavedBlock[F[_]: Async]: EitherT[F, String, Option[BlockSavedLog]] =
    println("getLastSavedBlock")
    inline def latestQuery = quote { query[BlockSavedLog].filter(t => t.isBuild).sortBy(t => t.number)(Ord.desc).take(1) }
    optionQuery[F, BlockSavedLog](latestQuery)
  
  def getLastBuildedBlock[F[_]: Async](): EitherT[F, String, Option[BlockStateEntity]] =
    inline def latestQuery = quote { query[BlockStateEntity].filter(t => t.isBuild).sortBy(t => t.number)(Ord.desc).take(1) }
    optionQuery[F, BlockStateEntity](latestQuery)

    
  def blockDataSize[F[_]: Async]: EitherT[F, String, Option[Long]] =
    // val rawQuery = quote {
    //   (id: Int) => sql"""SELECT id, name FROM my_entity WHERE id = $id""".as[Query[(Int, String)]]
    // }
    // SELECT x._1, x._2 FROM (SELECT id AS "_1", name AS "_2" FROM my_entity WHERE id = 1) x
    inline def sizeQuery = quote { sql"""select sum(pg_column_size('json')) from block_state""".as[Query[Long]] }
    optionQuery[F, BlockStateEntity](sizeQuery)
  /*
val a = quote {
  liftQuery(List(Person(0, "John", 31),Person(0, "name2", 32)))
    .foreach(e => query[Person].insert(_.name -> p.name, _.age -> p.age))
    .returning(_.id)
}
   */
