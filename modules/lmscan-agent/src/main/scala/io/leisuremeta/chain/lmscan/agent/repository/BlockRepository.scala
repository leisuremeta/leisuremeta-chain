package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{BlockSavedLog}
import io.leisuremeta.chain.lmscan.agent.entity.BlockEntity
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
    inline def latestQuery = quote { query[BlockSavedLog].sortBy(t => t.eventTime)(Ord.desc).take(1) }
    optionQuery[F, BlockSavedLog](latestQuery)
  
  /*
val a = quote {
  liftQuery(List(Person(0, "John", 31),Person(0, "name2", 32)))
    .foreach(e => query[Person].insert(_.name -> p.name, _.age -> p.age))
    .returning(_.id)
}
   */
