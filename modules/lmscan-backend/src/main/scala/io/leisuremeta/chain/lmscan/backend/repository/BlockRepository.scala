package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.common.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.entity.Block
import cats.data.EitherT
import cats.effect.Async
import cats.implicits.*
import io.getquill.*
import java.sql.SQLException
object BlockRepository extends CommonQuery:

  import ctx.*
  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
      cnt: Long,
  ): EitherT[F, String, Seq[Block]] =
    def pagedQuery =
      quote { (offset: Long, sizePerRequest: Int) =>

        query[Block]
          .filter(t => t.number <= offset)
          .sortBy(t => t.number)(Ord.desc)
          .take(sizePerRequest)
      }

    val sizePerRequest = pageNavInfo.sizePerRequest
    val offset         = sizePerRequest * pageNavInfo.pageNo
    seqQuery(pagedQuery(lift(cnt - offset), lift(sizePerRequest)))

  def getLast[F[_]: Async](): EitherT[F, String, Option[Block]] =
    inline def q =
      quote { () =>
        query[Block]
          .sortBy(t => t.number)(Ord.desc)
          .take(1)
      }
    optionQuery(q())

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
