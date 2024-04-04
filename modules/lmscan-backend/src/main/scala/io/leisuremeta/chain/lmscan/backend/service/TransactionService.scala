package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.common.model._
import io.leisuremeta.chain.lmscan.backend.repository._
import cats.data.EitherT
import cats.effect.Async

object TransactionService:

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Tx]] =
    TransactionRepository.get(hash)

  def getDetail[F[_]: Async](
      hash: String,
  ): EitherT[F, Either[String, String], Option[TxDetail]] =
    for
      trx <- TransactionRepository.get(hash).leftMap(Left(_))
      detail = trx.map { tx =>
        val outputValsOpt: Option[Seq[TransferHist]] = tx.outputVals match
          // [버그리포트] 개발망 devscan 트랜잭션 detail 표시 에러 hot fix
          case Some(outputValSeq) =>
            outputValSeq(0) == "/" match
              case true => None
              case _ =>
                Some(outputValSeq.map { (outputVal: String) =>
                  val items = outputVal.split("/")
                  TransferHist(
                    Some(items(0)),
                    Some(items(1)),
                  )
                })
          case None => None
        TxDetail(
          Some(tx.hash),
          Some(tx.eventTime),
          Some(tx.fromAddr),
          Some(tx.txType),
          Some(tx.tokenType),
          tx.inputHashs,
          outputValsOpt,
          Some(tx.json),
          Some(tx.subType),
        )
      }
    yield detail

  // EitherT[F[_], A, B] is a lightweight wrapper for F[Either[A, B]]
  // that makes it easy to compose Eithers and Fs together.
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      summaryOpt <- SummaryService.get(0)
      page <- TransactionRepository.getPage(pageNavInfo).leftMap(Left(_))
      summary = summaryOpt.getOrElse(SummaryModel())
      cnt = summary.totalTxSize.getOrElse(0L)
      txInfo = convertToInfo(page)
    yield PageResponse.from(cnt, pageNavInfo.sizePerRequest, txInfo)

  def getPageBySubtype[F[_]: Async](
      subType: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPageBySubtype(subType, pageNavInfo).leftMap(Left(_))
      txInfo = convertToInfo(page.payload)
    yield PageResponse(page.totalCount, page.totalPages, txInfo)

  def getPageByAccount[F[_]: Async](
      address: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, Either[String, String], PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPageByAccount(address, pageNavInfo).leftMap(Left(_))
      // page <- TransactionRepository.getPageByAccount(address).leftMap(Left(_))
    // yield txInfo
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
    subType match
      case None =>
        (accountAddr, blockHash) match
          case (None, None) =>
            getPage[F](pageInfo)
          case (_, _) =>
            EitherT.left(Async[F].delay(Left("검색 파라미터를 하나만 입력해주세요.")))
      case Some(subtype) =>
        getPageBySubtype[F](subtype, pageInfo)

  def convertToInfo(txs: Seq[Tx]): Seq[TxInfo] =
    txs.map { tx =>
      val latestOutValOpt = tx.outputVals match
        case Some(seq) =>
          val x = seq.map(_.split("/"))
          x.headOption.map(x =>
            if x.isEmpty then ""
            else x(1),
          )
        case None => None

      TxInfo(
        Some(tx.hash),
        Some(tx.blockNumber),
        Some(tx.eventTime),
        Some(tx.txType),
        Some(tx.tokenType),
        Some(tx.fromAddr),
        Some(tx.subType),
        None,
        latestOutValOpt,
      )
    }
  def countTotalTx[F[_]: Async] =
    for cnt <- TransactionRepository.countTotalTx
      //   txInfo = convertToInfoForAccount(page.payload, address)
    yield Some(cnt)

  def convertToInfoForAccount(txs: Seq[Tx], address: String): Seq[TxInfo] =
    txs.map { tx =>
      val latestOutValOpt = tx.outputVals match
        // case Some(seq) => seq.map(_.split("/")).headOption.map(_(1))
        case Some(seq) =>
          val x = seq.map(_.split("/"))
          x.headOption.map(x =>
            if x.isEmpty then ""
            else x(1),
          )
        case None => None

      TxInfo(
        Some(tx.hash),
        Some(tx.blockNumber),
        Some(tx.eventTime),
        Some(tx.txType),
        Some(tx.tokenType),
        Some(tx.fromAddr),
        Some(tx.subType),
        Some(if tx.fromAddr == address then "Out" else "In"),
        latestOutValOpt,
      )
    }
