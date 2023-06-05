package io.leisuremeta.chain.lmscan
package backend2

import io.circe.generic.auto.*

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
// import sttp.tapir.EndpointIO
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import common.LmscanApi
import common.ExploreApi
import common.model.{PageNavigation, SummaryModel}

// import io.leisuremeta.chain.lmscan.backend.service.*
import io.leisuremeta.chain.lmscan.backend2

import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.decorator.CorsDecorator;
import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import sttp.tapir.server.armeria.TapirService
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerOptions
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.interceptor.cors.CORSConfig
import io.leisuremeta.chain.lmscan.common.ExploreApi.baseEndpoint
import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import doobie.*
import doobie.implicits.*
import doobie.util.ExecutionContexts
import cats.*
import cats.data.*
import cats.effect.*
import cats.implicits.*
import fs2.Stream

import sttp.tapir.model.StatusCodeRange.ServerError
import io.leisuremeta.chain.lmscan.common.ExploreApi.UserError
import io.leisuremeta.chain.lmscan.common.ExploreApi.ServerError
import io.leisuremeta.chain.lmscan.common.model.dao.Tx
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.genEither
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
// import io.leisuremeta.chain.lmscan.common.model.Utills.Dao2Dto

object BackendMain extends IOApp:

// Endpoint[Unit, Unit, Either[ServerError, UserError], Seq[DTO_Tx], Any]
// Full[Unit, Unit, Unit, Either[ServerError, UserError], Seq[DTO_Tx], Any, F]
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx")
      .out(jsonBody[List[DTO_Tx]])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx page")
        Queries.getTx
          .pipe(QueriesPipe.pipeTx[F])
          .leftMap { (errMsg) =>
            scribe.error(s"errorMsg: $errMsg")
            (ExploreApi
              .ServerError(errMsg.toString()))
              .asLeft[ExploreApi.UserError]
          }
          .value
      }

  // def tx2[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
  //   baseEndpoint.get
  //     .in("tx2")
  //     .out(jsonBody[List[DTO_Tx]])
  //     .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
  //       scribe.info(s"get tx page")
  //       getTx2[F](
  //       ).leftMap { (errMsg) =>
  //         scribe.error(s"errorMsg: $errMsg")
  //         // (ExploreApi.ServerError("errMsg")).asLeft[ExploreApi.UserError]
  //         errMsg
  //       }
  //     }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      tx[F],
      // hash[F],
      // summaryMain[F],
    )

  def getServerResource[F[_]: Async]: Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(Async[F].async_[Server] { cb =>

        val options = ArmeriaCatsServerOptions
          .customiseInterceptors(dispatcher)
          .corsInterceptor(Some {
            CORSInterceptor
              .customOrThrow[F](CORSConfig.default)
          })
          .options

        val tapirService = ArmeriaCatsServerInterpreter[F](options)
          .toService(explorerEndpoints[F])
        val server = Server.builder
          .annotatedService(tapirService)
          .http(8081)
          .maxRequestLength(128 * 1024 * 1024)
          .requestTimeout(java.time.Duration.ofMinutes(6))
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

  override def run(
      args: List[String],
  ): IO[ExitCode] =

    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
