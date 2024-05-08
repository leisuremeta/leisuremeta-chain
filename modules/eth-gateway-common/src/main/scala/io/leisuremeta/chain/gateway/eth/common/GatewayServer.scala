package io.leisuremeta.chain.gateway.eth.common

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.effect.std.Dispatcher
import cats.syntax.flatMap.*

import com.linecorp.armeria.server.Server
import scodec.bits.ByteVector
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.interceptor.log.DefaultServerLog

import client.{GatewayDatabaseClient, GatewayKmsClient}

object GatewayServer:
  def gatewayServerEndpoint[F[_]: Async](
      dbClient: GatewayDatabaseClient[F],
      kmsClient: GatewayKmsClient[F],
  ): ServerEndpoint.Full[
    Unit,
    Unit,
    GatewayApi.GatewayRequest,
    GatewayApi.GatewayApiError,
    GatewayApi.GatewayResponse,
    Any,
    F,
  ] =
    def decrypt(
        key: String,
        doublyEncryptedFrontPartBase64: String,
    ): EitherT[F, String, Resource[F, Array[Byte]]] =
      for
        frontBytes <- EitherT.fromEither[F]:
          ByteVector.fromBase64Descriptive(doublyEncryptedFrontPartBase64)
        doublyEncryptedBackPartBase64Option <- dbClient.select(key)
        backPartBase64 <- EitherT.fromOption(
          doublyEncryptedBackPartBase64Option,
          s"Value not found for key: ${key}",
        )
        backBytes <- EitherT.fromEither[F]:
          ByteVector.fromBase64Descriptive(backPartBase64)
        plaintextResource <- kmsClient.decrypt:
          (frontBytes ++ backBytes).toArrayUnsafe
      yield plaintextResource

    GatewayApi.postDecryptEndpoint.serverLogic: request =>
      decrypt(
        request.key,
        request.doublyEncryptedFrontPartBase64,
      ).value
        .flatMap:
          case Left(errorMsg) =>
            Async[F].delay:
              Left(GatewayApi.ServerError(errorMsg))
          case Right(resource) =>
            resource.use: plaintext =>
              Async[F].delay:
                Right(
                  GatewayApi.GatewayResponse(
                    ByteVector.view(plaintext).toBase64,
                  ),
                )

  def make[F[_]: Async](
      dispatcher: Dispatcher[F],
      port: Int,
      dbClient: GatewayDatabaseClient[F],
      kmsClient: GatewayKmsClient[F],
  ): Resource[F, Server] =
    val serverEndpoint = gatewayServerEndpoint[F](dbClient, kmsClient)
    Resource.fromAutoCloseable(getServer(dispatcher, port, serverEndpoint))

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def getServer[F[_]: Async](
      dispatcher: Dispatcher[F],
      port: Int,
      serverEndpoint: ServerEndpoint[Fs2Streams[F], F],
  ): F[Server] = Async[F].fromCompletableFuture:

    def log[F[_]: Async](
        level: scribe.Level,
    )(msg: String, exOpt: Option[Throwable])(using
        mdc: scribe.mdc.MDC,
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
    Async[F].delay:
      server.start().thenApply(_ => server)
