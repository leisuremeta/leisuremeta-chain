package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.backend.entity.TxState
import io.leisuremeta.chain.lmscan.common.model._
import io.leisuremeta.chain.lmscan.backend.repository._
import cats.data.EitherT
import cats.effect.Async

object TransactionService:

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[TxState]] =
    TransactionRepository.get(hash)

  def getDetail[F[_]: Async](
      hash: String,
  ): EitherT[F, Either[String, String], Option[TxDetail]] =
    for
      trx <- TransactionRepository.get(hash).leftMap(Left(_))
      detail = trx.map { tx =>
        TxDetail(
          Some(tx.hash),
          Some(tx.eventTime),
          Some(tx.json),
        )
      }
    yield detail

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      summaryOpt <- SummaryService.get(0)
      page <- TransactionRepository.getPage(pageNavInfo).leftMap(Left(_))
      summary = summaryOpt.getOrElse(SummaryModel())
      cnt = Math.min(summary.totalTxSize.getOrElse(0L), 100000L)
      txInfo = convertToInfo(page)
    yield PageResponse.from(cnt, pageNavInfo.sizePerRequest, txInfo)

  def getPageByAccount[F[_]: Async](
      address: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPageByAccount(address, pageNavInfo).leftMap(Left(_))
    yield page.copy(payload = convertToInfoForAccount(page.payload, address))

  def getPageByBlock[F[_]: Async](
      blockHash: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getTxPageByBlock(blockHash, pageNavInfo).leftMap(Left(_))
    yield page.copy(payload = convertToInfo(page.payload))

  def getPageByFilter[F[_]: Async](
      pageInfo: PageNavigation,
      accountAddr: Option[String],
      blockHash: Option[String],
      subType: Option[String],
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    (accountAddr, blockHash) match
      case (None, None) =>
        getPage[F](pageInfo)
      case (_, _) =>
        EitherT.left(Async[F].delay(Left("검색 파라미터를 하나만 입력해주세요.")))

  def convertToInfo(txs: Seq[Tx]): Seq[TxInfo] =
    txs.map { tx =>
      TxInfo(
        Some(tx.hash),
        Some(tx.blockNumber),
        Some(tx.eventTime),
        Some(tx.txType),
        Some(tx.tokenType),
        Some(tx.signer),
        Some(tx.subType),
        None,
        None,
      )
    }

  def convertToInfoForAccount(txs: Seq[Tx], address: String): Seq[TxInfo] =
    txs.map { tx =>
      TxInfo(
        Some(tx.hash),
        Some(tx.blockNumber),
        Some(tx.eventTime),
        Some(tx.txType),
        Some(tx.tokenType),
        Some(tx.signer),
        Some(tx.subType),
        Some(if tx.signer == address then "Out" else "In"),
        None,
      )
    }
