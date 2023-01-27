package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.NftFile
import cats.effect.kernel.Async
import scala.concurrent.ExecutionContext
import cats.data.EitherT
import io.getquill.*

object NftFileRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async](
      tokenId: String,
  )(using ExecutionContext): EitherT[F, String, Option[NftFile]] =
    inline def detailQuery =
      quote { (tokenId: String) =>
        query[NftFile].filter(f => f.tokenId == tokenId).take(1)
      }
    optionQuery(detailQuery(lift(tokenId)))
