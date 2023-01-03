package io.leisuremeta.chain.lmscan
package backend

import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.functor.toFunctorOps

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import common.LmscanApi

object BackendMain extends IOApp:

  def helloServerEndpoint[F[_]]: ServerEndpoint[Fs2Streams[F], F] =
    LmscanApi.helloEndpoint.serverLogicPure { _ =>
      Right(LmscanApi.Utf8("Hello world!"))
    }

  def explorerEndpoints[F[_]]: List[ServerEndpoint[Fs2Streams[F], F]] = List(
    helloServerEndpoint[F],
  )

  def getServerResource[F[_]: Async]: Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(Async[F].async_[Server] { cb =>
        val tapirService = ArmeriaCatsServerInterpreter[F](dispatcher)
          .toService(explorerEndpoints[F])
        val server = Server.builder
          .http(8081)
          .maxRequestLength(128 * 1024 * 1024)
          .requestTimeout(java.time.Duration.ofMinutes(10))
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

  override def run(args: List[String]): IO[ExitCode] =
    val program =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
