package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation

object BlockService:
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Block]] =
    BlockRepository.getPage(pageNavInfo)
