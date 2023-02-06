package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
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
  // def apply[F[_]: BlockRepository]: BlockRepository[F] = summon

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, PageResponse[Block]] =
    val cntQuery = quote {
      query[Block]
    }

    def pagedQuery =
      quote { (pageNavInfo: PageNavigation) =>
        val offset         = sizePerRequest * pageNavInfo.pageNo
        val sizePerRequest = pageNavInfo.sizePerRequest

        query[Block]
          .sortBy(t => t.eventTime)(Ord.desc)
          .drop(offset)
          .take(sizePerRequest)
      }

    val res = for
      a <- countQuery(cntQuery)
      b <- seqQuery(pagedQuery(lift(pageNavInfo)))
    yield (a, b)

    res.map { (totalCnt, r) =>
      val totalPages = calTotalPage(totalCnt, pageNavInfo.sizePerRequest)
      new PageResponse(totalCnt, totalPages, r)
    }

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
