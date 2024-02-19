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
  import ctx.{*, given}

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]] =

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .take(sizePerRequest)
      }

    seqQuery(pagedQuery(lift(pageNavInfo)))

  def getPageBySubtype[F[_]: Async](
      subtype: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
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
    inline def pagedQuery =
      quote { (address: String) =>
        query[Tx]
          .filter(t =>
            t.fromAddr == lift(addr) || t.toAddr.contains(lift(addr)),
          )
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .take(20)
      }
    for
      seq <- seqQuery(pagedQuery(lift(addr)))
      size = seq.size
    yield PageResponse(size, 1, seq)

  def getTxPageByBlock[F[_]: Async](
      blockHash: String,
  ): EitherT[F, String, Seq[Tx]] =
    def pagedQuery =
      quote { (hash: String) =>

        query[Tx]
          .filter(t => t.blockHash == hash)
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
      }

    seqQuery(pagedQuery(lift(blockHash)))

  def countTotalTx[F[_]: Async]: EitherT[F, String, Long] =
    val cntQuery = quote {
      query[Tx]
    }

    for cnt <- countQuery(cntQuery)
    yield cnt
