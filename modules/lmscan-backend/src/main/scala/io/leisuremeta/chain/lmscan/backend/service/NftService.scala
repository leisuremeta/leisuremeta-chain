package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.entity.Nft
import io.leisuremeta.chain.lmscan.backend.repository._
import io.leisuremeta.chain.lmscan.common.model._
import cats.implicits.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.backend.repository.NftOwnerRepository
import io.leisuremeta.chain.lmscan.backend.entity._
object NftService:
  def getNftDetail[F[_]: Async](
      tokenId: String, // tokenId
  ): EitherT[F, String, Option[NftDetail]] =
    for
      page <- NftRepository.getPageByTokenId(
        tokenId,
        new PageNavigation(0, 10),
      )
      activities = page.payload.map(nft =>
        NftActivity(
          Some(nft.txHash),
          Some(nft.action),
          Some(nft.fromAddr),
          Some(nft.toAddr),
          Some(nft.eventTime),
        ),
      )
      nftOwner <- NftOwnerRepository.get(tokenId)

      // nftFile <- NftFileRepository.get(tokenId)
      nft <- NftFileRepository.get(tokenId)
      nftFile = nft.map(nftFile =>
        NftFileModel(
          Some(nftFile.tokenId),
          Some(nftFile.tokenDefId),
          Some(nftFile.collectionName),
          Some(nftFile.nftName),
          Some(nftFile.nftUri),
          Some(nftFile.creatorDescription),
          Some(nftFile.dataUrl),
          Some(nftFile.rarity),
          Some(nftFile.creator),
          Some(nftFile.eventTime),
          Some(nftFile.createdAt),
          Some(nftOwner.getOrElse(new NftOwner).owner),
        ),
      )
    yield Some(NftDetail(nftFile, Some(activities)))

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[NftInfoModel]] =
    for 
      page <- NftInfoRepository.getPage(pageNavInfo)
      nftInfos = page.payload.map { info =>
        NftInfoModel(
          tokenDefId = Some(info.tokenDefId),
          season = Some(info.season),
          collectionName = Some(info.collectionName),
          collectionSn = Some(info.collectionSn),
          totalSupply = info.totalSupply,
          startDate = info.startDate.map(_.toInstant()),
          endDate = info.endDate.map(_.toInstant()),
        )
      }
    yield PageResponse(page.totalCount, page.totalPages, nftInfos)
  def getSeasonPage[F[_]: Async](
      pageNavInfo: PageNavigation,
      tokenId: String,
  ): EitherT[F, String, PageResponse[NftSeasonModel]] =
    for 
      page <- NftInfoRepository.getSeasonPage(pageNavInfo, tokenId)
      seasons = page.payload.map(_.toModel)
    yield PageResponse(page.totalCount, page.totalPages, seasons)
