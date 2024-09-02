package io.leisuremeta.chain.lmscan
package backend
package repository

import entity._
import common.model._
import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.leisuremeta.chain.lmscan.backend.entity.NftSeason
import entity.NftInfo
import java.net.URLDecoder

object NftInfoRepository extends CommonQuery:
  import ctx.*

  def getSeasonPage[F[_]: Async](
      pageNavInfo: PageNavigation,
      seasonEnc: String,
  ): EitherT[F, String, PageResponse[NftSeason]] =
    val season = URLDecoder.decode(seasonEnc, "UTF-8")
    val cntQuery = quote:
      (season: String) => query[NftFile]
        .join(query[CollectionInfo].filter(s => s.season == season))
        .on((n, c) => n.tokenDefId == c.tokenDefId)

    def pagedQuery =
      quote: (pageNavInfo: PageNavigation, season: String) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[NftFile]
          .join(query[CollectionInfo].filter(s => s.season == season))
          .on((n, c) => n.tokenDefId == c.tokenDefId)
          .map((n, c) => 
            NftSeason(
              n.nftName,
              n.tokenId,
              n.tokenDefId,
              n.creator,
              n.rarity,
              n.dataUrl,
              c.collectionName,
            ) 
          )
          .sortBy(s => s.tokenId)(Ord.asc)
          .drop(offset)
          .take(sizePerRequest)

    val res = for
      a <- countQuery(cntQuery(lift(season)))
      b <- seqQuery(pagedQuery(lift(pageNavInfo), lift(season)))
    yield (a, b)

    res.map: (totalCnt, r) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, r)

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[NftInfo]] =
    val cntQuery = quote: 
      query[NftInfo]

    def pagedQuery =
      quote: (pageNavInfo: PageNavigation) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[NftInfo]
          .sortBy(t => t.sort)(Ord.asc)
          .drop(offset)
          .take(sizePerRequest)

    val res = for
      a <- countQuery(cntQuery)
      b <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (a, b)

    res.map: (totalCnt, r) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, r)

  def get[F[_]: Async](
      tokenId: String,
  ): EitherT[F, String, Option[CollectionInfo]] =
    inline def detailQuery =
      quote: (tokenId: String) => 
        query[CollectionInfo].filter(f => f.tokenDefId == tokenId).take(1)
    optionQuery(detailQuery(lift(tokenId)))
