package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.common.model.dao.*
// import io.leisuremeta.chain.lmscan.backend.entity.Tx
import cats.data.EitherT
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.Literal
import cats.effect.{Async, IO}
import scala.concurrent.Future
trait TransactionRepository[F[_]]:
  def getPage(
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]]

object TransactionRepository extends CommonQuery:

  import ctx.{*, given}

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  def getTx[F[_]: Async](): EitherT[F, String, Seq[Tx]] =
    // optionQuery(quote {
    //   query[Tx]
    //     // .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
    //     .take(2)
    // })
    seqQuery(quote {
      query[Tx]
        // .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
        .take(2)
    })
