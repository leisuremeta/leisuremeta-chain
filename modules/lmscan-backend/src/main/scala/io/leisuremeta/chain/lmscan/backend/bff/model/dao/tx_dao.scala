package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.Tx
import cats.data.EitherT
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.Literal
import cats.effect.{Async, IO}
import scala.concurrent.Future

object Dao extends CommonQuery:
  import ctx.{*, given}
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    // OFFSET 시작번호, limit 페이지보여줄갯수
    val cntQuery = quote {
      query[Tx]
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .take(sizePerRequest)
      }

    val res = for
      a <- countQuery(cntQuery)
      b <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (a, b)

    res.map { (totalCnt, r) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, 777, r)
    }
