package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT

import io.leisuremeta.chain.lmscan.backend.repository.AccountRepository
import io.leisuremeta.chain.lmscan.backend.service.TransactionService
import io.leisuremeta.chain.lmscan.backend.entity.{Tx, Account}
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
  ): EitherT[F, Either[String, String], Option[AccountDetail]] =
    for
      account <- AccountRepository.get(address).leftMap:
        e => Left(e)
      txPage <- TransactionService.getPageByAccount(
        address,
        new PageNavigation(0, 20),
      )
      res <- account match
        case Some(x) => 
          EitherT.rightT[F, Either[String, String]](
            Some(AccountDetail(
              Some(x.address),
              Some(x.balance),
              Some(x.amount),
              Some(txPage.payload),
            ))
          )
        case None =>
          EitherT.leftT[F, Option[AccountDetail]](Right(s"$address is not exist"))
    yield res

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[AccountInfo]] =
    for 
      page <- AccountRepository.getPage(pageNavInfo).leftMap(Left(_))
      summary <- SummaryRepository.get().leftMap(Left(_))
      price = summary match
        case Some(s) => s.lmPrice
        case None => 0.0
      accInfos = page.payload.map((b) =>
        val balance = b.free + b.locked
        AccountInfo(
          Some(b.address),
          Some(balance),
          Some(Instant.ofEpochSecond(b.updatedAt)),
          Some(balance / BigDecimal("1E+18") * BigDecimal(price))
          
        )
      )
    yield PageResponse(page.totalCount, page.totalPages, accInfos)
