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
import io.leisuremeta.chain.lmscan.agent.entity.Tx
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

  inline def insertWithoutTransaction[F[_]: Async, T](
      tx: Tx,
  ): EitherT[F, String, Long] =
    println("222222")
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          ids <- Async[F]
            .fromCompletableFuture(Async[F].delay {
              println("333333")
              ctx.transaction[Long] {
                for p <- ctx
                    .run(
                      quote {
                        query[Tx]
                          .insertValue(
                            lift(tx),
                          )
                          .onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash)
                      },
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

  // inline def insert[F[_]: Async, T](): EitherT[F, String, Long] =
  //   println("222222")
  //   EitherT {
  //     Async[F].recover {
  //       for
  //         given ExecutionContext <- Async[F].executionContext
  //         ids <- Async[F]
  //           .fromCompletableFuture(Async[F].delay {
  //             println("333333")
  //             ctx.transaction { implicit ec =>
  //               for p <- ctx
  //                   .run(
  //                     quote {
  //                       query[Tx].insertValue(
  //                         lift(
  //                           Tx(
  //                             "123",
  //                             "account",
  //                             "1234",
  //                             "123a",
  //                             Instant.now().toEpochMilli(),
  //                           ),
  //                         ),
  //                       )
  //                     },
  //                   )
  //               yield p
  //             // ctx.run(query)
  //             }
  //           })
  //           .map(Either.right(_))
  //       yield
  //         scribe.info("444444")
  //         ids
  //     } {
  //       case e: SQLException =>
  //         scribe.info("55555")
  //         Left(s"sql exception occured: " + e.getMessage())
  //       case e: Exception =>
  //         scribe.info("66666: " + e.getMessage())
  //         Left(e.getMessage())
  //     }
  //   }

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
