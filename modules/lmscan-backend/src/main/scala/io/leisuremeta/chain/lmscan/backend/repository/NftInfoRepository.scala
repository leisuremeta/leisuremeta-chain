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

object NftInfoRepository extends CommonQuery:
  import ctx.{*, given}

  def getSeasonPage[F[_]: Async](
      pageNavInfo: PageNavigation,
      season: String,
  ): EitherT[F, String, PageResponse[NftSeason]] =
    val cntQuery = quote:
      (season: String) => query[NftFile]
        .join(query[CollectionInfo].filter(s => s.season == season))
        .on((n, c) => n.tokenDefId == c.tokenDefId)

    def pagedQuery =
      quote: (pageNavInfo: PageNavigation, season: String) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[Nft]
          .join(query[NftFile])
          .on((n, f) => n.tokenId == f.tokenId)
          .join(query[CollectionInfo].filter(s => s.season == season))
          .on((n, c) => n._2.tokenDefId == c.tokenDefId)
          .map((n, _) => 
            NftSeason(
              n._2.nftName,
              n._1.tokenId,
              n._2.tokenDefId,
              n._2.creator,
              n._2.rarity,
              n._2.dataUrl,
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
