package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.common.model.dao.*

import cats.data.EitherT
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.Literal
import cats.effect.{Async, IO}
import scala.concurrent.Future
import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.leisuremeta.chain.lmscan.common.model.Utills.*
trait TransactionRepository[F[_]]:
  def getPage(
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]]

object TransactionRepository extends CommonQuery:

  import ctx.{*, given}

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  // http://localhost:8081/tx?pipe=(take(3),absend,asd,asd,asd)&dto=(txDetailpage)&view=(form)
  def getTx[F[_]: Async](): EitherT[F, String, Seq[DTO_Tx]] =
    seqQuery(quote {
      query[Tx]
        .take(2)
    })
      .map(dao => dao2dto(dao))

  def getPageByTokenId[F[_]: Async](
      tokenId: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Nft]] =
    val cntQuery = quote {
      query[Nft]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo
        // val orderBy        = pageNavInfo.orderBy()

        query[Nft]
          .filter(t => t.tokenId == lift(tokenId))
          .sortBy(t => t.eventTime)(Ord.desc)
          .drop(offset)
          .take(sizePerRequest)
      }

    val res = for
      a <- countQuery(cntQuery)
      b <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (a, b)

    res.map { (totalCnt, r) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, r)
    }
