package io.leisuremeta.chain.lmscan.backend.service

import cats.effect.kernel.Async
import cats.data.EitherT
import scala.concurrent.ExecutionContext

import io.leisuremeta.chain.lmscan.backend.repository.AccountRepository
import io.leisuremeta.chain.lmscan.backend.service.TransactionService
import io.leisuremeta.chain.lmscan.backend.entity.{Tx, Account}
import io.leisuremeta.chain.lmscan.backend.model.{
  PageNavigation,
  PageResponse,
  AccountDetail,
}

object AccountService:
  def get[F[_]: Async](
      address: String,
  )(using ExecutionContext): EitherT[F, String, Option[AccountDetail]] =
    val accountOpt = AccountRepository.get(address)
    accountOpt.map {
      (account: Option[Account]) /*Either[String, Option[Account]]*/ =>
        val detail = new AccountDetail(account.get)

        val txPage = TransactionService.getPage(new PageNavigation(true, 0, 20))
        txPage.value match
          case Left(errMsg)          => scribe.info(s"$errMsg")
          case Right(value: Seq[Tx]) => detail.txHistory = value

        Some(detail)
    }
