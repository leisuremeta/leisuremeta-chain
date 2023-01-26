package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.entity.Block
import io.leisuremeta.chain.lmscan.backend.repository.BlockRepository
import scala.concurrent.ExecutionContext

object BlockService:
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Block]] =
    BlockRepository.getPage(pageNavInfo)

  def get[F[_]: Async](
      hash: String,
  )(using ExecutionContext): EitherT[F, String, Option[Block]] =
    BlockRepository.get(hash)
