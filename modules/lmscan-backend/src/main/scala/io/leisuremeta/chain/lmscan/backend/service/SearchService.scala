package io.leisuremeta.chain.lmscan.backend.service

import io.leisuremeta.chain.lmscan.common.model._
import cats.data.EitherT
import cats.effect.Async

object SearchService:
  def getKeywordSearch[F[_]: Async](keyword: String): EitherT[F, Either[String, String], SearchResult] =
    keyword.toLongOption match
      case Some(n) => numSearch(n)
      case None => hashSearch(keyword)

  def numSearch[F[_]: Async](n: Long): EitherT[F, Either[String, String], SearchResult] =
    for
      nft <- NftService.getNftDetail(n.toString)
      blc <- BlockService.getByNumber(n)
      res = (nft, blc) match
        case (Some(d), _) if d.nftFile.isDefined => SearchResult.nft(d)
        case (_, Some(d)) => SearchResult.blc(d)
        case _ => SearchResult.empty
    yield res

  def hashSearch[F[_]: Async](keyword: String): EitherT[F, Either[String, String], SearchResult] =
    for
      tx <- TransactionService.getDetail(keyword)
      blc <- BlockService.getDetail(keyword, 1)
      acc <- AccountService.get(keyword, 1)
      res = (tx, blc, acc) match
        case (Some(d), _, _) if d.json.isDefined => SearchResult.tx(d)
        case (_, Some(d), _) if d.number.isDefined => SearchResult.blc(d)
        case (_, _, Some(d)) if d.totalCount > 0 => SearchResult.acc(d)
        case _ => SearchResult.empty
    yield res
