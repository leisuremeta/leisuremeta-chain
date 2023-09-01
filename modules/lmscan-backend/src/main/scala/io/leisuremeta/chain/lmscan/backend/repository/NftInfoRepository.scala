package io.leisuremeta.chain.lmscan
package backend
package repository

import entity.NftInfo
import common.model._
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*

object NftInfoRepository extends CommonQuery:
  import ctx.{*, given}

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[NftInfo]] =
    val cntQuery = quote {
      query[NftInfo]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[NftInfo]
          .sortBy(t => t.tokenDefId)(Ord.asc)
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
      tokenId: String,
  ): EitherT[F, String, Option[NftInfo]] =
    inline def detailQuery =
      quote { (tokenId: String) =>
        query[NftInfo].filter(f => f.tokenDefId == tokenId).take(1)
      }
    optionQuery(detailQuery(lift(tokenId)))
