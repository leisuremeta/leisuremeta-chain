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
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import common.LmscanApi
import common.ExploreApi
// import common.model.{PageNavigation, SummaryModel}

import io.leisuremeta.chain.lmscan.common.model.*
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
import io.leisuremeta.chain.lmscan.common.model.Dao2Dto
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.genEither
import io.leisuremeta.chain.lmscan.backend2.CatsUtil.eitherToEitherT
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import cats.instances.unit

object BackendMain extends IOApp:

  // http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxPageEndPoint = baseEndpoint.get
    .in("tx" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .in(
      query[Option[String]]("accountAddr")
        .and(query[Option[String]]("blockHash"))
        .and(query[Option[String]]("subtype")),
    )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def summary[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("summary")
      .in("main")
      .out(jsonBody[DTO.Summary.SummaryMain])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get summary")
        SummaryQuery.getSummary
          .pipe(QueriesPipe.pipeSummary[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx")
      .in(
        query[Option[String]]("pipe"),
      )
      .out(jsonBody[List[DTO.Tx.type1]])
      .serverLogic { (pipe) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx with pipe=$pipe")
        TxQuery
          .getTxPipe(pipe)
          .pipe(QueriesPipe.pipeTx[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  def txCount[F[_]: Async] =
    baseEndpoint.get
      .in("tx")
      .in("count")
      .out(jsonBody[DTO.Tx.count])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx count")
        CountQuery
          .getTxCount()
          .pipe(QueriesPipe.pipeTxCount[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  // account/{addr}/detail
  // (1) account.address == addr
  // (2) tx.addr == addr
  // (1) + (2)
  // @SuppressWarnings(Array("org.wartremover.warts.Any"))
  // def account[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
  //   baseEndpoint.get
  //     .in("account")
  //     .in(path[String])
  //     .in("detail")
  //     .out(jsonBody[DAO.Account])
  //     .serverLogic { (address: String) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
  //       scribe.info(s"get Account")
  //       AccountQuery.getAccount
  //         .pipe(QueriesPipe.pipeAccount[F])
  //         .pipe(ErrorHandle.genMsg)
  //         .value
  //     }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def accountDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("account")
      .in(path[String])
      .in("detail")
      .out(jsonBody[DTO.Account.Detail])
      .serverLogic { (address: String) =>
        scribe.info(s"get Account11")
        AccountService
          .getAccountDetail(address)
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      tx[F],
      // account[F],
      accountDetail[F],
      txCount[F],
      summary[F],
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
