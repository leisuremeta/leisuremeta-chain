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
import cats.effect.IO
import cats.effect.kernel.Async
import io.leisuremeta.chain.lmscan.backend.model.TxInfo

object TransactionService:

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Tx]] =
    TransactionRepository.get(hash)

  def getDetail[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[TxDetail]] =
    for
      trx <- TransactionRepository.get(hash)
      detail = trx.map { tx =>
        val outputValsOpt: Option[Seq[TransferHist]] = tx.outputVals match
          case Some(outputValSeq) =>
            Some(outputValSeq.map { (outputVal: String) =>
              val items = outputVal.split("/")
              TransferHist(
                items(0),
                items(1),
              )
            })
          case None => None
        TxDetail(
          tx.hash,
          tx.eventTime,
          tx.fromAddr,
          tx.txType,
          tx.tokenType,
          tx.inputHashs,
          outputValsOpt,
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
      txInfo = convertToInfoForAccount(page.payload, address)
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
      accountAddr: Option[String],
      blockHash: Option[String],
  ): EitherT[F, String, PageResponse[TxInfo]] =
    (accountAddr, blockHash) match
      case (None, None) =>
        getPage[F](pageInfo)
      case (None, Some(blockHash)) =>
        getPageByBlock[F](blockHash, pageInfo)
      case (Some(accountAddr), None) =>
        getPageByAccount[F](accountAddr, pageInfo)
      case (_, _) =>
        EitherT.left(Async[F].delay("검색 파라미터를 하나만 입력해주세요."))

  def convertToInfo(txs: Seq[Tx]): Seq[TxInfo] =
    txs.map { tx =>
      val latestOutValOpt = tx.outputVals match
        case Some(seq) => seq.map(_.split("/")).headOption.map(_(1))
        case None      => None
      TxInfo(
        tx.hash,
        tx.blockNumber,
        tx.eventTime,
        tx.txType,
        tx.tokenType,
        tx.fromAddr,
        None,
        latestOutValOpt,
      )
    }

  def convertToInfoForAccount(txs: Seq[Tx], address: String): Seq[TxInfo] =
    txs.map { tx =>
      val latestOutValOpt = tx.outputVals match
        case Some(seq) => seq.map(_.split("/")).headOption.map(_(1))
        case None      => None
      TxInfo(
        Some(tx.hash),
        Some(tx.blockNumber),
        Some(tx.eventTime),
        Some(tx.txType),
        Some(tx.tokenType),
        Some(tx.fromAddr),
        Some(if tx.fromAddr == address then "Out" else "In"),
        latestOutValOpt,
      )
    }
