package io.leisuremeta.chain.lmscan.agent.repository

import cats.data.EitherT
import cats.effect.kernel.Async

import java.sql.SQLException
import cats.implicits.*
import cats.effect.{Async, IO}
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.getquill.Literal
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.{global as ec}
import io.getquill.Query
import java.time.Instant

trait CommonQuery:
  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")
  import ctx.{*, given}

  inline def optionQuery[F[_]: Async, T](
      inline query: Query[T],
  ): EitherT[F, String, Option[T]] =
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          detail <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(query)
            })
        yield Right(detail.headOption)
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }
  inline def insertTransaction[F[_]: Async, T](
        inline query: Insert[T]
    ): EitherT[F, String, Long] =
      scribe.info("222222")
      EitherT {
        Async[F].recover {
          for
            given ExecutionContext <- Async[F].executionContext
            ids <- Async[F]
              .fromCompletableFuture(Async[F].delay {
                scribe.info("333333")
                ctx.transaction[Long] {
                  for p <- ctx
                      .run(
                        // quote {
                        //   query[T]
                        //     .insertValue(
                        //       lift(entity),
                        //     )
                        //     .onConflictUpdate(_.id())((t, e) => t.id() -> e.id())
                        // },
                        query
                      )
                  yield p
                }
              })
              .map(Either.right(_))
          yield
            scribe.info("444444")
            ids
        } {
          case e: SQLException =>
            scribe.info("55555")
            Left(s"sql exception occured: " + e.getMessage())
          case e: Exception =>
            scribe.info("66666: " + e.getMessage())
            Left(e.getMessage())
        }
      }

  inline def countQuery[F[_]: Async, T](
      inline query: Query[T],
  ): EitherT[F, String, Long] =
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          result <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(query.size)
            })
            .map(Either.right(_))
        yield
          scribe.info(s"countQuery Result: $result")
          result
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }



  inline def insertBatch[F[_]: Async, T](
      // inline query: Insert[T],
      inline query: BatchAction[Insert[T]],
  ): EitherT[F, String, Seq[Long]] =
    EitherT {
      Async[F].recover {
        for
          // given ExecutionContext <- Async[F].executionContext
          ids <- Async[F]
            .fromCompletableFuture(Async[F].delay {
              ctx.transaction { implicit ec =>
                for p <- ctx.run(query)
                yield p
              // ctx.run(query)
              }
            })
            .map(Either.right(_))
        yield ids
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception =>
          Left(e.getMessage())
      }
    }

  inline def seqQuery[F[_]: Async, T](
      inline query: Query[T],
  ): EitherT[F, String, Seq[T]] =
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
              ctx.run(query)
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
    }
