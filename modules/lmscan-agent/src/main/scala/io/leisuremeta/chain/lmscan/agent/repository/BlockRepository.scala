package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{Block, BlockSavedLog}
import io.getquill.*

object BlockRepository extends CommonQuery:

  import ctx.{*, given}

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Block]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[Block].filter(b => b.hash == hash).take(1)
      }
    optionQuery(detailQuery(lift(hash)))

  // def insert[F[_]: Async](block: Block): EitherT[F, String, Long] =
  //   println("11111")
  //   insertTransaction(block)

  def getLastSavedBlock[F[_]: Async]: EitherT[F, String, Option[BlockSavedLog]] =
    inline def latestQuery =
      quote { 
        query[BlockSavedLog].sortBy(t => t.eventTime)(Ord.desc).take(1)
      }
    optionQuery(latestQuery)
  
  /*
val a = quote {
  liftQuery(List(Person(0, "John", 31),Person(0, "name2", 32)))
    .foreach(e => query[Person].insert(_.name -> p.name, _.age -> p.age))
    .returning(_.id)
}
   */
