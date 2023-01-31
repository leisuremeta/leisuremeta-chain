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

trait CommonQuery:
  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")

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

  inline def insert[F[_]: Async, T](
      inline query: Insert[T],
  ): EitherT[F, String, Long] =
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          ids <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(query)
              // InternalApi.runBatchActionReturning(query)
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
