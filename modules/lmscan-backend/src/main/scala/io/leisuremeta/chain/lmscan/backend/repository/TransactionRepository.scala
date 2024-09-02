package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.backend.entity.TxState
import io.leisuremeta.chain.lmscan.backend.entity.AccountMapper
import cats.data.EitherT
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.*
import cats.effect.Async
import scala.concurrent.Future

trait TransactionRepository[F[_]]:
  def getPage(
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]]

object TransactionRepository extends CommonQuery:
  import ctx.*

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]] =

    inline def pagedQuery =
      quote { (offset: Int, sizePerRequest: Int) =>
        query[Tx]
          .sortBy(t => (t.blockNumber, t.eventTime))(Ord(Ord.desc, Ord.desc))
          .drop(offset)
          .take(sizePerRequest)
      }

    val sizePerRequest = pageNavInfo.sizePerRequest
    val offset         = sizePerRequest * pageNavInfo.pageNo
    seqQuery(pagedQuery(lift(offset), lift(sizePerRequest)))

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[TxState]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[TxState].filter(tx => tx.hash == hash).take(1)
      }

    optionQuery(detailQuery(lift(hash)))

  def getPageByAccount[F[_]: Async](
      addr: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    val cntQuery = quote {
      query[AccountMapper].filter(t => t.address == lift(addr))
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .join(
            query[AccountMapper]
              .filter(t => t.address == lift(addr))
              .sortBy(_.eventTime)(Ord.desc)
              .drop(offset)
              .take(sizePerRequest)
          )
          .on((tx, mapper) => tx.hash == mapper.hash)
          .map((tx, _) => tx)
      }

    val res = for
      totalCnt <- countQuery(cntQuery)
      payload  <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (totalCnt, payload)

    res.map { (totalCnt, payload) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, payload)
    }

  def getTxPageByBlock[F[_]: Async](
      hash: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    val cntQuery = quote {
      query[Tx].filter(t => t.blockHash == lift(hash))
    }

    inline def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val sizePerRequest = pageNavInfo.sizePerRequest
        val offset         = sizePerRequest * pageNavInfo.pageNo

        query[Tx]
          .filter(t => t.blockHash == lift(hash))
          .drop(offset)
          .take(sizePerRequest)
          .sortBy(t => t.eventTime)(Ord.desc)
      }

    val res = for
      totalCnt <- countQuery(cntQuery)
      payload  <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (totalCnt, payload)

    res.map { (totalCnt, payload) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, payload)
    }
