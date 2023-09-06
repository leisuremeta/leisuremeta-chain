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
