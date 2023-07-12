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

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .filter(t => t.display_yn == true)
          .take(sizePerRequest)
      }

    val res = for
      totalCnt <- countQuery(cntQuery)
      payload  <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (totalCnt, payload)

    res.map { (totalCnt, payload) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, payload)
    }

  def getPageBySubtype[F[_]: Async](
      subtype: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    // OFFSET 시작번호, limit 페이지보여줄갯수
    val cntQuery = quote {
      query[Tx].filter(t => t.subType == lift(subtype))
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .filter(t => t.subType == lift(subtype))
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .filter(t => t.display_yn == true)
          .take(sizePerRequest)
      }

    val res = for
      totalCnt <- countQuery(cntQuery)
      payload  <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (totalCnt, payload)

    res.map { (totalCnt, payload) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, payload)
    }

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Tx]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[Tx].filter(tx => tx.hash == hash).take(1)
      // query[Tx].filter(tx => tx.subType == "CreateAccount").take(1)
      }

    optionQuery(detailQuery(lift(hash)))

  def getPageByAccount[F[_]: Async](
      addr: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    val cntQuery = quote {
      query[Tx]
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo
        // val orderBy        = pageNavInfo.orderBy()

        query[Tx]
          .filter(t =>
            t.fromAddr == lift(addr) || t.toAddr.contains(lift(addr)),
          )
          .filter(t => t.display_yn == true)
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
      new PageResponse(totalCnt, totalPages, r)
    }

  def getTxPageByBlock[F[_]: Async](
      blockHash: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    val cntQuery = quote {
      query[Tx]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .filter(t => t.blockHash == lift(blockHash))
          .filter(t => t.display_yn == true)
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
      new PageResponse(totalCnt, totalPages, r)
    }

  // EitherT {
  //   Async[F].recover {
  //     for tx <- Async[F]
  //         .fromFuture(Async[F].delay {
  //           ctx.run(detailQuery(lift(hash)))
  //         })
  //     yield Right(tx.headOption)
  //   } {
  //     case e: SQLException =>
  //       Left(s"sql exception occured: " + e.getMessage())
  //     case e: Exception => Left(e.getMessage())
  //   }
  // }

// inline def run[T](inline quoted: Quoted[Query[T]]): Future[Seq[T]]
//   = InternalApi.runQueryDefault(quoted)

/*
    처음 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 0

    11번째부터 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 10
 */
