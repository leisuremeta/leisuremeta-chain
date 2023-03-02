package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.{TxDetail, TransferHist, TxInfo}
import io.leisuremeta.chain.lmscan.backend.repository.TransactionRepository
import cats.Functor
import cats.data.EitherT
import cats.Monad
import eu.timepit.refined.boolean.False
import cats.effect.Async
// import io.leisuremeta.ExploreApi
import io.leisuremeta.chain.lmscan.common.ExploreApi
import cats.implicits.catsSyntaxEitherId
import cats.effect.IO
import cats.effect.kernel.Async

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
//         final case class TxDetail(
//     hash: Option[String] = None,
//     createdAt: Option[Long] = None,
//     signer: Option[String] = None,
//     txType: Option[String] = None,
//     tokenType: Option[String] = None,
//     inputHashs: Option[Seq[String]] = None,
//     transferHist: Option[Seq[TransferHist]] = None,
//     json: Option[String] = None,
//     subType: Option[String] = None,
// )

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

  def getPageBySubtype[F[_]: Async](
      subType: String,
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[TxInfo]] =
    for
      page <- TransactionRepository.getPageBySubtype(subType, pageNavInfo)
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
      subType: Option[String],
  ): EitherT[F, String, PageResponse[TxInfo]] =
    subType match
      case None =>
        (accountAddr, blockHash) match
          case (None, None) =>
            getPage[F](pageInfo)
          case (None, Some(blockHash)) =>
            getPageByBlock[F](blockHash, pageInfo)
          case (Some(accountAddr), None) =>
            getPageByAccount[F](accountAddr, pageInfo)
          case (_, _) =>
            EitherT.left(Async[F].delay("검색 파라미터를 하나만 입력해주세요."))
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
        Some(if tx.fromAddr == address then "Out" else "In"),
        latestOutValOpt,
      )
    }
