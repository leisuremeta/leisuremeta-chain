package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.NftFile
import io.leisuremeta.chain.lmscan.backend.entity.Nft
import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*

object NftRepository extends CommonQuery:

  import ctx.{*, given}

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
