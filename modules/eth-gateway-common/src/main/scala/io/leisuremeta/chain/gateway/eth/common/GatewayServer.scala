package io.leisuremeta.chain.gateway.eth.common

import cats.effect.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.functor.*

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.interceptor.log.DefaultServerLog

object GatewayServer:
  def make[F[_]: Async](
      dispatcher: Dispatcher[F],
      port: Int,
      serverEndpoint: ServerEndpoint[Fs2Streams[F], F],
  ): Resource[F, Server] =
    Resource.make(getServer(dispatcher, port, serverEndpoint)): server =>
      Async[F]
        .fromCompletableFuture(Async[F].delay(server.closeAsync()))
        .map(_ => ())

  def getServer[F[_]: Async](
      dispatcher: Dispatcher[F],
      port: Int,
      serverEndpoint: ServerEndpoint[Fs2Streams[F], F],
  ): F[Server] = Async[F].async_[Server]: cb =>

    def log[F[_]: Async](
        level: scribe.Level,
    )(msg: String, exOpt: Option[Throwable])(using
        mdc: scribe.data.MDC,
    ): F[Unit] = Async[F].delay:
      exOpt match
        case None     => scribe.log(level, mdc, msg)
        case Some(ex) => scribe.log(level, mdc, msg, ex)

    val serverLog = DefaultServerLog(
      doLogWhenReceived = log(scribe.Level.Info)(_, None),
      doLogWhenHandled = log(scribe.Level.Info),
      doLogAllDecodeFailures = log(scribe.Level.Info),
      doLogExceptions =
        (msg: String, ex: Throwable) => Async[F].delay(scribe.warn(msg, ex)),
      noLog = Async[F].pure(()),
    )

    val serverOptions = ArmeriaCatsServerOptions
      .customiseInterceptors[F](dispatcher)
      .serverLog(serverLog)
      .options

    val tapirService = ArmeriaCatsServerInterpreter[F](serverOptions)
      .toService(serverEndpoint)

    val server = Server.builder
      .maxRequestLength(128 * 1024 * 1024)
      .requestTimeout(java.time.Duration.ofMinutes(10))
      .http(port)
      .service(tapirService)
      .build
      
    server.start.handle[Unit]:
      case (_, null)  => cb(Right(server))
      case (_, cause) => cb(Left(cause))
