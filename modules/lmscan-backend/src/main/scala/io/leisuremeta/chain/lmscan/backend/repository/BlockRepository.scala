package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.repository.CommonQuery
import io.leisuremeta.chain.lmscan.backend.entity.Block
import cats.data.EitherT
import cats.effect.{Async, IO}
import cats.implicits.*
import io.getquill.*
import io.getquill.Literal
import java.sql.SQLException
object BlockRepository extends CommonQuery:

  import ctx.{*, given}
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Block]] =
    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[Block]
          .sortBy(t => t.number)(Ord.desc)
          .drop(offset)
          .take(sizePerRequest)
      }
    seqQuery(pagedQuery(lift(pageNavInfo)))

  def get[F[_]: Async](
      hash: String,
  ): EitherT[F, String, Option[Block]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[Block].filter(b => b.hash == hash).take(1)
      }
    optionQuery(detailQuery(lift(hash)))

  def getByNumber[F[_]: Async](
      number: Long
  ): EitherT[F, String, Option[Block]] =
    inline def detailQuery =
      quote { (number: Long) =>
        query[Block].filter(b => b.number == number).take(1)
      }
    optionQuery(detailQuery(lift(number)))
