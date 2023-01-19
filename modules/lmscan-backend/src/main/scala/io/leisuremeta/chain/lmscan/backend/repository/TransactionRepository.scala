package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
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
import scala.concurrent.ExecutionContext.global
import java.sql.SQLException
import scala.concurrent.ExecutionContext

trait TransactionRepository[F[_]]:
  def getPage(
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]]

object TransactionRepository extends CommonQuery:

  // given scala.concurrent.ExecutionContext =
  //   scala.concurrent.ExecutionContext.global

  import ctx.{*, given}

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    // OFFSET 시작번호, limit 페이지보여줄갯수
    val cntQuery = quote {
      query[Tx]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo
        // val orderBy        = pageNavInfo.orderBy()

        query[Tx]
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

  def get[F[_]: Async](
      hash: String,
  )(using ExecutionContext): EitherT[F, String, Option[Tx]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[Tx].filter(tx => tx.hash == hash).take(1)
      }

    optionQuery(detailQuery(lift(hash)))

  def getPageByAccount[F[_]: Async](
      pageNavInfo: PageNavigation,
      addr: String,
  )(using ExecutionContext): EitherT[F, String, PageResponse[Tx]] =
    val cntQuery = quote {
      query[Tx]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo
        // val orderBy        = pageNavInfo.orderBy()

        query[Tx]
          .filter(t =>
            t.fromAddr == lift(addr) || t.toAddr.contains(lift(addr)),
          )
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

/*
    처음 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 0

    11번째부터 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 10
 */
