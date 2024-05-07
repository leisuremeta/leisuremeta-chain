package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.backend.repository.AccountRepository
import io.leisuremeta.chain.lmscan.common.model.{
  PageNavigation,
  PageResponse,
  AccountDetail,
}
import io.leisuremeta.chain.lmscan.common.model.AccountInfo
import java.time.Instant
import io.leisuremeta.chain.lmscan.backend.repository.SummaryRepository

object AccountService:
  def get[F[_]: Async](
      address: String,
      p: Int,
  ): EitherT[F, Either[String, String], Option[AccountDetail]] =
    for
      account <- AccountRepository.get(address).leftMap:
        e => Left(e)
      txPage <- TransactionService.getPageByAccount(
        address,
        PageNavigation(p - 1, 20),
      )
      summary <- SummaryRepository.get().leftMap(Left(_))
      price = summary match
        case Some(s) => BigDecimal(s.head.lmPrice)
        case None => BigDecimal(0)
      res <- account match
        case Some(x) => 
          EitherT.rightT[F, Either[String, String]](
            Some(AccountDetail(
              Some(x.address),
              Some(x.free),
              Some(x.free / BigDecimal("1E+18") * price),
              txPage.totalCount,
              txPage.totalPages,
              txPage.payload,
            )
          ))
        case None =>
          EitherT.rightT[F, Either[String, String]](
            Some(AccountDetail(
              Some(address),
              None,
              None,
              txPage.totalCount,
              txPage.totalPages,
              txPage.payload,
            )
          ))
    yield res

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[AccountInfo]] =
    for 
      page <- AccountRepository.getPage(pageNavInfo).leftMap(Left(_))
      summary <- SummaryRepository.get().leftMap(Left(_))
      price = summary match
        case Some(s) => s.headOption match
          case Some(h) => BigDecimal(h.lmPrice)
          case None => BigDecimal(0)
        case None => BigDecimal(0)
      accInfos = page.payload.map((b) =>
        val balance = b.free
        AccountInfo(
          Some(b.address),
          Some(balance),
          Some(Instant.ofEpochSecond(b.updatedAt)),
          Some(balance / BigDecimal("1E+18") * price),
        )
      )
    yield PageResponse(page.totalCount, page.totalPages, accInfos)
