package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.entity.Nft
import io.leisuremeta.chain.lmscan.backend.repository.{
  NftRepository,
  NftFileRepository,
}
import io.leisuremeta.chain.lmscan.common.model.{PageNavigation, PageResponse}
import io.leisuremeta.chain.lmscan.common.model.NftActivity
import io.leisuremeta.chain.lmscan.common.model.NftDetail
import io.leisuremeta.chain.lmscan.common.model.NftFileModel
import cats.implicits.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.backend.repository.NftOwnerRepository
import io.leisuremeta.chain.lmscan.backend.entity.NftOwner
import io.leisuremeta.chain.lmscan.backend.entity.NftOwnerModel
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
