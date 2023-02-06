package io.leisuremeta.chain.lmscan.backend.repository



object SummaryRepository extends CommonQuery:
  import ctx.{*, given}

  def get[F[_]: Async]: EitherT[F, String, Option[NftFile]] =
    inline def detailQuery =
      quote { (tokenId: String) =>
        query[NftFile].filter(f => f.tokenId == tokenId).take(1)
      }
    optionQuery(detailQuery(lift(tokenId)))
