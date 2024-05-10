package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.{BlockDetail, BlockInfo}
import io.leisuremeta.chain.lmscan.backend.entity.Block
import io.leisuremeta.chain.lmscan.backend.repository.BlockRepository
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object BlockService:
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[BlockInfo]] =
    for 
      summaryOpt <- SummaryService.get(0)
      page <- BlockRepository.getPage(pageNavInfo).leftMap:
        e => Left(e)
      summary = summaryOpt.getOrElse(SummaryModel())
      cnt = summary.blockNumber.getOrElse(0L)
      blockInfos = page.map { block =>
        BlockInfo(
          Some(block.number),
          Some(block.hash),
          Some(block.txCount),
          Some(block.eventTime),
        )
      }
    yield PageResponse.from(cnt, pageNavInfo.sizePerRequest, blockInfos)

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, Either[String, String], Option[Block]] =
    BlockRepository.get(hash).leftMap(Left(_))
  
  def getByNumber[F[_]: Async](
      number: Long,
      p: Int = 1,
  ): EitherT[F, Either[String, String], Option[BlockDetail]] =
    for
      block <- BlockRepository.getByNumber(number).leftMap(Left(_))
      txPage <- TransactionService.getPageByBlock(
        block.map(_.hash).getOrElse(""),
        PageNavigation(p - 1, 20),
      )
      blockInfo = block.map: bl =>
        BlockDetail(
          Some(bl.hash),
          Some(bl.parentHash),
          Some(bl.number),
          Some(bl.eventTime),
          Some(bl.txCount),
          txPage.totalCount,
          txPage.totalPages,
          txPage.payload,
        )
    yield blockInfo

  def getDetail[F[_]: Async](
      hash: String,
      p: Int,
  ): EitherT[F, Either[String, String], Option[BlockDetail]] =
    for
      block <- get(hash)
      txPage <- TransactionService.getPageByBlock(
        hash,
        PageNavigation(p - 1, 20),
      )
      blockInfo = block.map: bl =>
        BlockDetail(
          Some(bl.hash),
          Some(bl.parentHash),
          Some(bl.number),
          Some(bl.eventTime),
          Some(bl.txCount),
          txPage.totalCount,
          txPage.totalPages,
          txPage.payload,
        )
    yield blockInfo
