package io.leisuremeta.chain.lmscan
package backend

import cats.Monad
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.either.*
import cats.syntax.functor.toFunctorOps

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointIO
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import common.LmscanApi

import scala.collection.StringOps
import io.leisuremeta.ExploreApi
import io.leisuremeta.chain.lmscan.backend.service.TransactionService
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.repository.TransactionRepository
import cats.effect.Async
import scala.concurrent.ExecutionContext

object BackendMain extends IOApp:

  def txPaging[F[_]: Async](using
      ExecutionContext,
  ): ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      println(s"pageInfo: $pageInfo")
      val result = TransactionService
        .getPage[F](pageInfo)
        .leftMap { (errorMsg: String) =>
          println(s"errorMsg: $errorMsg")
          (ExploreApi.ServerError(errorMsg)).asLeft[ExploreApi.UserError]
        }
      // scribe.info(s"received getTxList request: $txHash")
      println(s"result.value: ${result.value}")
      result.value
    }

  def txDetail[F[_]: Async](using
      ExecutionContext,
  ): ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxDetailEndPoint.serverLogic { (hash: String) =>
      println(s"tx_hash: $hash")
      val result = TransactionService
        .get(hash)
        .leftMap { (errMsg: String) =>
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def blockPaging[F[_]: Async](using
      ExecutionContext,
  ): ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      val result = BlockService
        .getPage[F](pageInfo)

    }

  def accountDetail[F[_]: Async](using
      ExecutionContext,
  ): ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getAccountDetail.serverLogic { (address: String) =>
      scribe.info(s"accountDetail request address: $address")
      val result = AccountService
        .get(address)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def explorerEndpoints[F[_]: Async](using
      ExecutionContext,
  ): List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      txPaging[F],
      txDetail[F],
      blockPaging[F],
<<<<<<< HEAD
=======
      blockDetail[F],
      accountDetail[F],
>>>>>>> 4ef5fe9 (add account detail API interface)
    )

  def getServerResource[F[_]: Async](using
      ExecutionContext,
  ): Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(Async[F].async_[Server] { cb =>
        val tapirService = ArmeriaCatsServerInterpreter[F](dispatcher)
          .toService(explorerEndpoints[F])
        val server = Server.builder
          .http(8081)
          .maxRequestLength(128 * 1024 * 1024)
          .requestTimeout(java.time.Duration.ofSeconds(30))
          .service(tapirService)
          .build
        server.start.handle[Unit] {
          case (_, null)  => cb(Right(server))
          case (_, cause) => cb(Left(cause))
        }
      }) { server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .void
      }
    yield server

  // override def run[F[_]: Async: Monad: TransactionRepository, IO](
  override def run(
      args: List[String],
  ): IO[ExitCode] =
    implicit val ec: scala.concurrent.ExecutionContext =
      scala.concurrent.ExecutionContext.global

    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
