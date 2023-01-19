package io.leisuremeta.chain.lmscan.backend.repository

import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.entity.Tx
import cats.data.EitherT
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.Literal

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import cats.effect.{Async, IO}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.global
import java.sql.SQLException
import scala.concurrent.ExecutionContext

trait TransactionRepository[F[_]]:
  def getPage(
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]]

object TransactionRepository:

  // given scala.concurrent.ExecutionContext =
  //   scala.concurrent.ExecutionContext.global
  // val server = EmbeddedPostgres.builder().start()
  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")

  import ctx.{*, given}

  // def test(): LoadConfig = util.LoadConfig("ctx")

  def apply[F[_]: TransactionRepository]: TransactionRepository[F] =
    summon

  def getPage[F[_]: Async](
      pageNavInfo: PageNavigation,
  ): EitherT[F, String, Seq[Tx]] =
    // OFFSET 시작번호, limit 페이지보여줄갯수
    val sizePerRequest = pageNavInfo.sizePerRequest
    val offset         = sizePerRequest * pageNavInfo.pageNo
    inline def pagedQuery =
      quote { (offset: Int, limit: Int) =>
        query[Tx].drop(offset).take(limit)
      }

<<<<<<< HEAD
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          result <- Async[F]
            .fromFuture(Async[F].delay {
              // scribe.info(s"Running page query...")
              // type T = Seq[Transaction]
              // inline val wrap = OuterSelectWrap.Default
              // inline val q    = pagedQuery(lift(offset), lift(sizePerRequest))
              // inline val quoted = q
              // val ca =
              //   io.getquill.context.ContextOperation
              //     .Factory[
              //       PostgresDialect,
              //       LowerCase,
              //       PrepareRow,
              //       ResultRow,
              //       Session,
              //       ctx.type,
              //     ](ctx.idiom, ctx.naming)
              //     .op[Nothing, T, Result[RunQueryResult[T]]] { arg =>
              //       val simpleExt = arg.extractor.requireSimple()
              //       ctx.executeQuery(arg.sql, arg.prepare, simpleExt.extract)(
              //         arg.executionInfo,
              //         io.getquill.context
              //           .DatasourceContextInjectionMacro[
              //             RunnerBehavior,
              //             Runner,
              //             ctx.type,
              //           ](context),
              //       )
              //     }
              // QueryExecution.apply(ca)(quoted, None, wrap)
              // InternalApi.runQuery(q, OuterSelectWrap.Default)
              // InternalApi.runQueryDefault(q)
              ctx.run(pagedQuery(lift(offset), lift(sizePerRequest)))
              // Future.successful(Seq.empty)
            })
            .map(Either.right(_))
        yield
          scribe.info(s"Result: $result")
          result
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
=======
    val res = for
      a <- countQuery(cntQuery)
      b <- pageQuery(pagedQuery(lift(pageNavInfo)))
    yield (a, b)

    res.map { (totalCnt, r) =>
      val totalPages =
        Math.ceil(totalCnt.toDouble / pageNavInfo.sizePerRequest).toInt;
      new PageResponse(totalCnt, totalPages, r)
>>>>>>> 279f5c4 (add pagingQuery)
    }

  def get[F[_]: Async](
      hash: String,
  )(using ExecutionContext): EitherT[F, String, Option[Tx]] =
    inline def detailQuery =
      quote { (hash: String) =>
        query[Tx].filter(tx => tx.hash == hash).take(1)
      }

    EitherT {
      Async[F].recover {
        for txs <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(detailQuery(lift(hash)))
            })
        yield Right(txs.headOption)
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }

// inline def run[T](inline quoted: Quoted[Query[T]]): Future[Seq[T]]
//   = InternalApi.runQueryDefault(quoted)

/*
    처음 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 0

    11번째부터 10개의 게시글(ROW)를 가져온다.
    SELECT * FROM BBS_TABLE LIMIT 10 OFFSET 10
 */
