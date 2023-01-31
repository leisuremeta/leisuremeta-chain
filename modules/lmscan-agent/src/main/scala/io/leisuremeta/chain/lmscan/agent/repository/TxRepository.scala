package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.entity.Tx
import cats.data.EitherT
import io.getquill.*
import cats.effect.IO
import io.getquill.Query
import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery

case class Person(name: String, age: Int)

object TxRepository extends CommonQuery:
  import ctx.{*, given}

  def insert[F[_]: Async](tx: Tx): EitherT[F, String, Boolean] =
    // inline def upsertQuery =
    //   quote { (tx: Tx) =>
    //     query[Tx]
    //       .insert(_.hash ->)
    //   }
    // inline def a = quote {
    //   liftQuery(List(Person("John", 31), Person("name2", 32)))
    //     .foreach(e => query[Person].insert(_.name -> e.name, _.age -> e.age))
    //     .returning(_.id)
    // }
    ???

  def insertBatch[F[_]: Async](txs: List[Tx]): EitherT[F, String, String] =
    val batchInsert = quote {
      liftQuery(lift(txs))
        .foreach(tx =>
          query[Tx]
            .insertValue(tx)
            .returning(x => x),
        )
    }

    // liftQuery(vips).foreach(v =>
    //   query[Person].insertValue(Person(v.first + v.last, v.age)).returning(_.id),
    // )

    super.insert(batchInsert)

// case class Product(id: Int, description: String, sku: Long)

/*
val a = quote {
  liftQuery(List(Person(0, "John", 31),Person(0, "name2", 32)))
    .foreach(e => query[Person].insert(_.name -> p.name, _.age -> p.age))
    .returning(_.id)
}
 */
