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
import io.leisuremeta.chain.lmscan.common.model.dao.Account
import io.leisuremeta.chain.lmscan.common.model.dto.DTO_Account
import io.leisuremeta.chain.lmscan.common.model.AccountDetail

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
  // .serverLogic {
  //   (
  //       pageInfo,
  //       accountAddr,
  //       blockHash,
  //       subType,
  //   ) =>

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx_test[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx")
      .in("test")
      .out(jsonBody[List[DTO_Tx_type1]])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx")
        Queries.getTx
          .pipe(QueriesPipe.pipeTx[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def tx_test2[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("tx")
      .in("test2")
      .out(jsonBody[List[DTO_Tx_type1]])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx2")
        Queries.getTx_byAddress
          .pipe(QueriesPipe.pipeTx[F])
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
      .out(jsonBody[List[DTO_Tx_type1]])
      .serverLogic { (pipe) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get tx with pipe=$pipe")
        Queries
          .getTxPipe(pipe)
          .pipe(QueriesPipe.pipeTx[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def account[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("account")
      .out(jsonBody[Account])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get Account")
        Queries.getAccount
          .pipe(QueriesPipe.pipeAccount[F])
          .pipe(ErrorHandle.genMsg)
          .value
      }

  def accountService[F[_]: Async] =
    val r = for
      account <- Queries.getAccount
        .pipe(QueriesPipe.pipeAccount[F])
      txList <- Queries.getTx.pipe(QueriesPipe.pipeTx[F])
    yield (account, txList)
    r.map { (account, txList) =>
      DTO_AccountDetail(
        address = account.address,
        balance = account.balance,
        value = account.balance,
        txList = Some(txList),
      )
    }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def accountDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    baseEndpoint.get
      .in("account")
      .in("detail")
      .out(jsonBody[DTO_AccountDetail])
      .serverLogic { (Unit) => // Unit 대신에 프론트에서 url 함수 넣을수 있게 할수있다.
        scribe.info(s"get Account")
        accountService
          .pipe(ErrorHandle.genMsg)
          .value
      }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      tx[F],
      tx_test[F],
      tx_test2[F],
      account[F],
      accountDetail[F],
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
