package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.repository.TransactionRepository
import cats.Functor
import cats.data.EitherT
import cats.Monad
import eu.timepit.refined.boolean.False
import cats.effect.Async
import scala.concurrent.ExecutionContext

object TransactionService:

  // EitherT[F[_], A, B] is a lightweight wrapper for F[Either[A, B]]
  // that makes it easy to compose Eithers and Fs together.
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    TransactionRepository.getPage(pageNavInfo)

  def getPageByAccount[F[_]: Async](
      pageNavInfo: PageNavigation,
      address: String,
  )(using ExecutionContext): EitherT[F, String, PageResponse[Tx]] =
    TransactionRepository.getPageByAccount(pageNavInfo, address)

  def get[F[_]: Async](
      hash: String,
  )(using ExecutionContext): EitherT[F, String, Option[Tx]] =
    TransactionRepository.get(hash)