package io.leisuremeta.chain.lmscan.backend.repository

import cats.data.EitherT
import cats.effect.kernel.Async
import io.getquill.Query
import java.sql.SQLException
import cats.implicits.*
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*

import scala.concurrent.ExecutionContext

trait CommonQuery:
  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")
  inline def seqQuery[F[_]: Async, T](
      inline query: Query[T],
  ): EitherT[F, String, Seq[T]] =
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          result <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(query)
            })
            .map(Either.right(_))
        yield result
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
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
          result
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }

  inline def optionSeqQuery[F[_]: Async, T](
      inline query: Query[T],
  ): EitherT[F, String, Option[Seq[T]]] =
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          detail <- Async[F]
            .fromFuture(Async[F].delay {
              ctx.run(query)
            })
          res = if detail.isEmpty then Right(None) else Right(Some(detail))
        yield res
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }
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
          res = if detail.isEmpty then Right(None) else Right(detail.headOption)
        yield res
      } {
        case e: SQLException =>
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception => Left(e.getMessage())
      }
    }

  def calTotalPage(totalCnt: Long, sizePerRequest: Integer): Integer =
    Math.ceil(totalCnt.toDouble / sizePerRequest).toInt;
