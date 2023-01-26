package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import scala.concurrent.ExecutionContext
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.entity.Nft
import io.leisuremeta.chain.lmscan.backend.model.NftActivity
import io.leisuremeta.chain.lmscan.backend.repository.NftRepository
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.NftDetail
import cats.implicits.*
import cats.effect.IO

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import com.amazonaws.regions.{Region, Regions}
object NftService:
  def fileDownload(nftAddr: String): String =

    val BUCKET_NAME      = "comm2"
    val FILE_PATH_PREFIX = "temp/collections"
    val FILE_NAME        = "###"
    val AWS_ACCESS_KEY   = "AKIAQUKA4EHLFN3PORMS"
    val AWS_SECRET_KEY   = "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"

    val awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)
    val amazonS3Client = new AmazonS3Client(awsCredentials)

    amazonS3Client.setRegion(
      Region.getRegion(Regions.AP_NORTHEAST_2),
    )

    // create a new bucket
    amazonS3Client.createBucket(BUCKET_NAME)

    // download file and read line by line
    val obj =
      amazonS3Client.getObject(BUCKET_NAME, FILE_PATH_PREFIX + FILE_NAME)

    val reader = new BufferedReader(
      new InputStreamReader(obj.getObjectContent()),
    )
    var line = reader.readLine
    while line != null do
      println(line)
      line +: reader.readLine
    line

  def getPageByTokenId[F[_]: Async](
      tokenId: String, // tokenId
  )(using ExecutionContext): EitherT[F, String, Seq[Nft]] =
    NftRepository.getPageByTokenId(tokenId, new PageNavigation(true, 0, 10))

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
  //     NftActivity(
  //       nft.txHash,
  //       nft.action,
  //       nft.fromAddr,
  //       nft.toAddr,
  //       nft.eventTime,
  //   )
