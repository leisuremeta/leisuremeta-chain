package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import scala.concurrent.ExecutionContext
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.entity.Nft
import io.leisuremeta.chain.lmscan.backend.model.NftActivity
import io.leisuremeta.chain.lmscan.backend.repository.{
  NftRepository,
  NftFileRepository,
}
import io.leisuremeta.chain.lmscan.backend.model.{PageNavigation, PageResponse}
import io.leisuremeta.chain.lmscan.backend.model.NftDetail
import cats.implicits.*
import cats.effect.IO

object NftService:

  def getNftDetail[F[_]: Async](
      tokenId: String, // tokenId
  )(using ExecutionContext): EitherT[F, String, Option[NftDetail]] =
    for
      page <- NftRepository.getPageByTokenId(
        tokenId,
        new PageNavigation(0, 10),
      )
      activities = page.payload.map(nft =>
        NftActivity(
          nft.txHash,
          nft.action,
          nft.fromAddr,
          nft.toAddr,
          nft.eventTime,
        ),
      )
      nftFile <- NftFileRepository.get(tokenId)
    yield Some(NftDetail(nftFile, activities))

    // page.sss
    // var z = new NftDetail()

  // def getPageByTokenId[F[_]: Async](
  //     tokenId: String, // tokenId
  // )(using ExecutionContext): EitherT[F, String, Option[NftDetail]] =
  //   val res =
  //     for
  //       nft <- NftRepository.getPageByTokenId(
  //         tokenId,
  //         new PageNavigation(true, 0, 10),
  //       )
  //       x <- nft.payload.traverse { (n: Nft) => toActivity(n) }
  //               .right()
  //     yield x

  //   res

  // def toActivity(nft: Nft): Either[String, NftActivity] =
  //   Either.right(
