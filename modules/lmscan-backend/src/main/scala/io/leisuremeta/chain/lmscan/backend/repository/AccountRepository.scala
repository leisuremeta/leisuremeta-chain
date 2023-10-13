package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.leisuremeta.chain.lmscan.backend.entity._
import io.getquill.autoQuote
import io.leisuremeta.chain.lmscan.common.model._

object AccountRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async](
      addr: String,
  ): EitherT[F, String, Option[Account]] =
    inline def detailQuery =
      quote { (addr: String) =>
        query[Account]
          .join(query[Balance])
          .on((a, b) => a.address == b.address)
          .filter(_._1.address == addr)
          .take(1)
          .map { case (a, b) =>
            Account(
              address = a.address,
              balance = b.free,
              amount = a.amount,
              createdAt = a.createdAt,
            )
          }
      }
    optionQuery(detailQuery(lift(addr)))

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Balance]] =
    val cntQuery = quote {
      query[Balance]
        .filter(_.address != "eth-gateway") // filter eth-gateway
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[Balance]
          .filter(_.address != "eth-gateway")
          .sortBy(a => a.updatedAt)(Ord.asc)
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
