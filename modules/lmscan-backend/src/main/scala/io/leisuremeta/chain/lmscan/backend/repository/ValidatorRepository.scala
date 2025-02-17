package io.leisuremeta.chain.lmscan.backend.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.leisuremeta.chain.lmscan.backend.entity._

object ValidatorRepository extends CommonQuery:
  import ctx.*

  def get[F[_]: Async](
      addr: String,
  ): EitherT[F, String, Option[ValidatorInfo]] =
    inline def detailQuery =
      quote { (addr: String) =>
        query[ValidatorInfo]
          .filter(_.address == addr)
          .take(1)
      }
    optionQuery(detailQuery(lift(addr)))

  def getPage[F[_]: Async](): EitherT[F, String, Seq[ValidatorInfo]] =
    inline def q = quote:
      query[ValidatorInfo]
        

    for
      res <- seqQuery(q)
    yield res
