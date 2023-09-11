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
import common.ExploreApi
import common.model.{PageNavigation, SummaryModel}

import scala.collection.StringOps
import io.leisuremeta.chain.lmscan.backend.service.*
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
    ExploreApi.getTxPageEndPoint.serverLogic {
      (
          pageInfo,
          accountAddr,
          blockHash,
          subType,
      ) =>
        scribe.info(s"txPaging request pageInfo: $pageInfo")
        TransactionService
          .getPageByFilter[F](pageInfo, accountAddr, blockHash, subType)
          .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def txDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxDetailEndPoint.serverLogic { (hash: String) =>
      scribe.info(s"txDetail request hash: $hash")
      TransactionService
        .getDetail(hash)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
}
  def blockPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      scribe.info(s"blockPaging request pageInfo: $pageInfo")
      BlockService
        .getPage[F](pageInfo)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def blockDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockDetailEndPoint.serverLogic { (hash: String) =>
      BlockService
        .getDetail(hash)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def accountPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getAccountPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      AccountService
        .getPage[F](pageInfo)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def accountDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getAccountDetailEndPoint.serverLogic { (address: String) =>
      AccountService
        .get(address)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def nftDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftDetailEndPoint.serverLogic { (tokenId: String) =>
      scribe.info(s"nftDetail request tokenId: $tokenId")
      NftService
        .getNftDetail(tokenId)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def nftSeasonPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftSeasonEndPoint.serverLogic { (tokenId: String, pageInfo: PageNavigation) =>
      scribe.info(s"nftSeasonPaging request pageInfo: $pageInfo")
      NftService
        .getSeasonPage[F](pageInfo, tokenId)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }
  def nftPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      scribe.info(s"nftPaging request pageInfo: $pageInfo")
      NftService
        .getPage[F](pageInfo)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def summaryMain[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getSummaryMainEndPoint.serverLogic { Unit =>
      scribe.info(s"summary request")
      SummaryService.getBoard
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def summaryChart[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getSummaryChartEndPoint.serverLogic { Unit =>
      scribe.info(s"summary chart request")
      SummaryService.getList
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def explorerEndpoints[F[_]: Async]: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      txPaging[F],
      txDetail[F],
      blockPaging[F],
      blockDetail[F],
      accountPaging[F],
      accountDetail[F],
      nftPaging[F],
      nftSeasonPaging[F],
      nftDetail[F],
      summaryMain[F],
      summaryChart[F],
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

  override def run(args: List[String]): IO[ExitCode] =
    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
