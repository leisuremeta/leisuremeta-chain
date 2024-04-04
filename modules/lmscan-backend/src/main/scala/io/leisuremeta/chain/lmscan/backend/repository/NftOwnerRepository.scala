package io.leisuremeta.chain.lmscan.backend.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.getquill.*
import io.leisuremeta.chain.lmscan.backend.entity.NftOwner

object NftOwnerRepository extends CommonQuery:
  import ctx.*

  def get[F[_]: Async](
      tokenId: String,
  ): EitherT[F, String, Option[NftOwner]] =
    inline def detailQuery =
      quote { (tokenId: String) =>
        query[NftOwner].filter(f => f.tokenId == tokenId).take(1)
      }
    optionQuery(detailQuery(lift(tokenId)))
