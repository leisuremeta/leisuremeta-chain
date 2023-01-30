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
import io.leisuremeta.ExploreApi
import cats.implicits.catsSyntaxEitherId

object TransactionService:

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Tx]] =
    TransactionRepository.get(hash)

  // EitherT[F[_], A, B] is a lightweight wrapper for F[Either[A, B]]
  // that makes it easy to compose Eithers and Fs together.
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    TransactionRepository.getPage(pageNavInfo)

  def getPageByAccount[F[_]: Async](
      address: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    TransactionRepository.getPageByAccount(address, pageNavInfo)

  def getPageByBlock[F[_]: Async](
      blockHash: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Tx]] =
    TransactionRepository.getTxPageByBlock(blockHash, pageNavInfo)

  def getPageByFilter[F[_]: Async](
      pageInfo: PageNavigation,
      blockHash: Option[String],
      accountAddr: Option[String],
  ): EitherT[F, String, PageResponse[Tx]] =
    (accountAddr, blockHash) match
      case (None, None) => getPage[F](pageInfo)
      case (None, Some(blockHash)) =>
        getPageByBlock[F](blockHash, pageInfo)
      case (Some(accountAddr), None) =>
        getPageByAccount[F](accountAddr, pageInfo)
      case (_, _) =>
        throw new RuntimeException("검색 파라미터를 하나만 입력해주세요.")
      // (ExploreApi
      //   .ServerError("검색 파라미터를 하나만 입력해주세요."))
      //   .asLeft[ExploreApi.UserError],
