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
import io.leisuremeta.chain.lmscan.backend.repository.PlaynommBalanceRepository

object AccountService:
  def get[F[_]: Async](
      address: String,
  ): EitherT[F, String, Option[AccountDetail]] =
    val res = for
      account <- AccountRepository.get(address)
      txPage <- TransactionService.getPageByAccount(
        address,
        new PageNavigation(0, 20),
      )
    yield (account, txPage)

    res.map { (accountOpt, page) =>
      accountOpt match
        case Some(x) =>
          val detail = AccountDetail(
            Some(x.address),
            Some(x.balance),
            Some(x.amount),
            Some(page.payload),
          )
          Some(detail)
        case None =>
          scribe.info(s"there is no exist account of '$address'")
          Option.empty
    }

    // accountOpt match
    //   case Some(x) => ???
    //   case None    => None
    // accountOpt.map {
    //   (account: Option[Account]) /*Either[String, Option[Account]]*/ =>
    //     val detail = new AccountDetail(account.get)

    //     val txPage: EitherT[F, String, PageResponse[Tx]] =
    //       TransactionService.getPage(new PageNavigation(true, 0, 20))
    //     // val z: Either[String, PageResponse[Tx]] = txPage.value
    //     txPage.value match
    //       case Left(errMsg) => scribe.info(s"$errMsg")
    //       case Right(res: PageResponse[Tx]) =>
    //         detail.txHistory = res.payload

    //     Some(detail)
