package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.model.{
  TxDetail,
  TransferHist,
  TxInfo,
}
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
  ): EitherT[F, String, Option[TxDetail]] =
    for
      trx <- TransactionRepository.get(hash)
      detail = trx.map { tx =>
        TxDetail(
          tx.hash,
          tx.createdAt,
          tx.fromAddr,
          tx.txType,
          tx.tokenType,
          tx.inputHashs,
          tx.outputVals.map { (ftv: String) =>
            val items = ftv.split("/")
            TransferHist(
              items(0),
              items(1),
            )
          },
          tx.json,
        )
      }
    yield detail

  // EitherT[F[_], A, B] is a lightweight wrapper for F[Either[A, B]]
  // that makes it easy to compose Eithers and Fs together.
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPage(pageNavInfo)
      txInfo = convertToInfo(page.payload)
    yield PageResponse(page.totalCount, page.totalPages, txInfo)

  def getPageByAccount[F[_]: Async](
      address: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPageByAccount(address, pageNavInfo)
      txInfo = convertToInfo(page.payload)
    yield PageResponse(page.totalCount, page.totalPages, txInfo)

  def getPageByBlock[F[_]: Async](
      blockHash: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getTxPageByBlock(blockHash, pageNavInfo)
      txInfo = convertToInfo(page.payload)
    yield PageResponse(page.totalCount, page.totalPages, txInfo)

  def getPageByFilter[F[_]: Async](
      pageInfo: PageNavigation,
      blockHash: Option[String],
      accountAddr: Option[String],
  ): EitherT[F, String, PageResponse[TxInfo]] =
    (accountAddr, blockHash) match
      case (None, None) => getPage[F](pageInfo)
      case (None, Some(blockHash)) =>
        getPageByBlock[F](blockHash, pageInfo)
      case (Some(accountAddr), None) =>
        getPageByAccount[F](accountAddr, pageInfo)
      case (_, _) =>
        throw new RuntimeException("검색 파라미터를 하나만 입력해주세요.")

  def convertToInfo(txs: Seq[Tx]): Seq[TxInfo] =
    txs.map { tx =>
      val latestOutVal = tx.outputVals.headOption match
        case Some(str) => str.split("/")
        case None      => Array[String]("", "")
      TxInfo(
        tx.hash,
        tx.blockNumber,
        tx.eventTime,
        tx.txType,
        tx.tokenType,
        tx.fromAddr,
        latestOutVal(1),
      )
    }
