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
    NftService.getNftDetail(n.toString)
      .flatMap: d =>
        EitherT.pure[F, Either[String, String]](SearchResult.nft(d.get))
      .leftFlatMap: _ =>
        for
          blc <- BlockService.getByNumber(n)
        yield SearchResult.blc(blc.get)
      .leftFlatMap: _ => 
        EitherT.pure[F, Either[String, String]](SearchResult.empty)

  def hashSearch[F[_]: Async](keyword: String): EitherT[F, Either[String, String], SearchResult] =
    TransactionService.getDetail(keyword)
      .flatMap: d =>
        EitherT.pure[F, Either[String, String]](SearchResult.tx(d.get))
      .leftFlatMap: _ =>
        for
          blc <- BlockService.getDetail(keyword, 1)
        yield SearchResult.blc(blc.get)
      .leftFlatMap: _ =>
        for
          acc <- AccountService.get(keyword, 1)
        yield  SearchResult.acc(acc.get)
      .leftFlatMap: _ => 
        EitherT.pure[F, Either[String, String]](SearchResult.empty)
