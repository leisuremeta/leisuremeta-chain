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
import io.leisuremeta.chain.lmscan.backend.service.*
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import cats.data.EitherT
import io.leisuremeta.chain.lmscan.backend.repository.TransactionRepository
import cats.effect.Async
import com.linecorp.armeria.server.cors.CorsService

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

object BackendMain extends IOApp:

  def txPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      scribe.info(s"txPaging request pageInfo: $pageInfo")
      val result = TransactionService
        .getPage[F](pageInfo)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      println(s"result.value: ${result.value}")
      result.value
    }

  def txDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxDetailEndPoint.serverLogic { (hash: String) =>
      scribe.info(s"txDetail request hash: $hash")
      val result = TransactionService
        .get(hash)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")

          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def blockPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      scribe.info(s"blockPaging request pageInfo: $pageInfo")
      val result = BlockService
        .getPage[F](pageInfo)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def blockDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockDetailEndPoint.serverLogic { (hash: String) =>
      scribe.info(s"blockDetail request hash: $hash")
      val result = BlockService
        .get(hash)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def accountDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
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

  def nftDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftDetail.serverLogic { (tokenId: String) =>
      scribe.info(s"nftDetail request tokenId: $tokenId")
      val result = NftService
        .getNftDetail(tokenId)
        .leftMap { (errMsg: String) =>
          scribe.error(s"errorMsg: $errMsg")
          (ExploreApi.ServerError(errMsg)).asLeft[ExploreApi.UserError]
        }
      result.value
    }

  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      txPaging[F],
      txDetail[F],
      blockPaging[F],
      blockDetail[F],
      accountDetail[F],
      nftDetail[F],
    )

  def getServerResource[F[_]: Async]: Resource[F, Server] =

    def corsService =
      CorsService
        .builder("*")
        .allowCredentials()
        .allowNullOrigin() // 'Origin: null' will be accepted.
        .allowRequestMethods(HttpMethod.POST, HttpMethod.GET)
        .allowRequestHeaders("allow_request_header")
        .exposeHeaders("expose_header_1", "expose_header_2")
        .preflightResponseHeader("x-preflight-cors", "CORS")
        .newDecorator();

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

    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
