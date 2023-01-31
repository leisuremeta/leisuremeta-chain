package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.agent.entity.Tx
import cats.data.EitherT
import io.getquill.*
import cats.effect.IO
import io.getquill.Query
import io.getquill.context.*
import io.leisuremeta.chain.lmscan.agent.repository.CommonQuery

case class Person(name: String, age: Int)

object TxRepository extends CommonQuery:
  import ctx.{*, given}

  def insert[F[_]: Async](tx: Tx): EitherT[F, String, Long] =
    inline def upsertQuery =
      quote { (tx: Tx) =>
        query[Tx].insertValue(tx)
      }
    super.insert(upsertQuery(lift(tx)))
    

