package io.leisuremeta.chain.lmscan.agent.repository

import cats.effect.kernel.Async
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.agent.entity.{NftTxEntity}
import io.getquill.*
  
object NftRepository extends CommonQuery:

  import ctx.{*, given}

  def getByTxHash[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[NftTxEntity]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[NftTxEntity].filter(b => b.txHash == hash).take(1)
      }
    optionQuery(detailQuery(lift(hash)))