package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.model.BlockDetail
import io.leisuremeta.chain.lmscan.backend.entity.Block
import io.leisuremeta.chain.lmscan.backend.repository.BlockRepository

object BlockService:
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Block]] =
    BlockRepository.getPage(pageNavInfo)

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Block]] =
    BlockRepository.get(hash)

  def getDetail[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[BlockDetail]] =
    for
      block <- get(hash)
      txPage <- TransactionService.getPageByBlock(
        hash,
        new PageNavigation(0, 10),
      )

      blockInfo = block.map { bl =>
        BlockDetail(
          bl.hash,
          bl.parentHash,
          bl.number,
          bl.eventTime,
          bl.txCount,
          txPage.payload,
        )
      }
    yield blockInfo
