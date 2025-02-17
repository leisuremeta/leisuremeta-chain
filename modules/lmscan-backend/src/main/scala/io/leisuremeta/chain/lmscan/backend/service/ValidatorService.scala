package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.common.model.{
  PageNavigation,
  PageResponse,
}
import io.leisuremeta.chain.lmscan.backend.repository.ValidatorRepository
import io.leisuremeta.chain.lmscan.common.model.NodeValidator
import io.leisuremeta.chain.lmscan.backend.repository.BlockRepository
import io.leisuremeta.chain.lmscan.backend.entity.Block
import com.typesafe.config.ConfigFactory

object ValidatorService:
  val conf = ConfigFactory.load()
  val vdcnt = conf.getInt("vdcnt")
  def get[F[_]: Async](
      address: String,
      p: Int,
  ): EitherT[F, Either[String, String], Option[NodeValidator.ValidatorDetail]] =
    for
      latestBlcOpt: Option[Block] <- BlockRepository.getLast().leftMap:
        e => Left(e)
      validator <- ValidatorRepository.get(address).leftMap(Left(_))
      cnt = latestBlcOpt.get.number
      start = cnt - ((p - 1) * vdcnt) * 20
      blcs <- BlockRepository.getPageByProposer(
        PageNavigation(p - 1, 20),
        start,
        address,
      ).leftMap(Left(_))
      res = validator.map(v => NodeValidator.ValidatorDetail(v.toModel, PageResponse.from(
          cnt / vdcnt, 20, blcs.map(_.toModel)
        )))
    yield res

  def getPage[F[_]: Async](): EitherT[F, Either[String, String], Seq[NodeValidator.Validator]] =
    for 
      page <- ValidatorRepository.getPage().leftMap(Left(_))
      res = page.map(_.toModel)
    yield res
