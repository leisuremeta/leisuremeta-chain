package io.leisuremeta.chain.lmscan
package backend

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.effect.std.Dispatcher
import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter
import sttp.tapir.*
import common.ExploreApi
import common.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.service.*
import cats.effect.Async
import com.linecorp.armeria.server.HttpService;
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerOptions
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.interceptor.cors.CORSConfig
import sttp.tapir.server.interceptor.log.DefaultServerLog

object BackendMain extends IOApp:

  def txPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxPageEndPoint.serverLogic {
      (
          pageInfo,
      ) =>
        TransactionService
          .getPage[F](pageInfo)
          .leftMap:
            case Right(msg) => Right(ExploreApi.BadRequest(msg))
            case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def txDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getTxDetailEndPoint.serverLogic { (hash: String) =>
      TransactionService
        .getDetail(hash)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }
  def blockPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      BlockService
        .getPage[F](pageInfo)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def blockDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getBlockDetailEndPoint.serverLogic { (hash: String, p: Option[Int]) =>
      BlockService
        .getDetail(hash, p.getOrElse(1))
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
    ExploreApi.getAccountDetailEndPoint.serverLogic { (address: String, p: Option[Int]) =>
      AccountService
        .get(address, p.getOrElse(1))
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def nftDetail[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftDetailEndPoint.serverLogic { (tokenId: String) =>
      NftService
        .getNftDetail(tokenId)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def nftSeasonPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftSeasonEndPoint.serverLogic { (season: String, pageInfo: PageNavigation) =>
      NftService
        .getSeasonPage[F](pageInfo, season)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }
  def nftPaging[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftPageEndPoint.serverLogic { (pageInfo: PageNavigation) =>
      NftService
        .getPage[F](pageInfo)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def nftOwnerInfo[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getNftOwnerInfoEndPoint.serverLogic { (tokenId: String) =>
      NftService
        .getNftOwnerInfo(tokenId)
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }


  def summaryMain[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getSummaryMainEndPoint.serverLogic { Unit =>
      SummaryService.getBoard
        .leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def summaryChart[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getSummaryChartEndPoint.serverLogic { (chartType: String) =>
      val list = chartType match
        case "balance" => SummaryService.getList
        case _ => SummaryService.get5List
      
      list.leftMap:
          case Right(msg) => Right(ExploreApi.BadRequest(msg))
          case Left(msg) => Left(ExploreApi.ServerError(msg))
        .value
    }

  def keywordSearch[F[_]: Async]: ServerEndpoint[Fs2Streams[F], F] =
    ExploreApi.getKeywordSearchResult.serverLogic:
      (keyword: String) =>
        SearchService
          .getKeywordSearch(keyword)
          .leftMap:
            case Right(msg) => Right(ExploreApi.BadRequest(msg))
            case Left(msg) => Left(ExploreApi.ServerError(msg))
          .value

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
      nftOwnerInfo[F],
      summaryMain[F],
      summaryChart[F],
      keywordSearch[F],
    )

  def getServerResource[F[_]: Async]: Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.fromAutoCloseable:
        def log[F[_]: Async](
            level: scribe.Level,
          )(msg: String, exOpt: Option[Throwable])(using
            mdc: scribe.mdc.MDC,
          ): F[Unit] =
            Async[F].delay(exOpt match
              case None     => scribe.log(level, mdc, msg)
              case Some(ex) => scribe.log(level, mdc, msg, ex),
          )
        val serverLog = DefaultServerLog(
          doLogWhenReceived = log(scribe.Level.Info)(_, None),
          doLogWhenHandled  = log(scribe.Level.Info),
          doLogAllDecodeFailures = log(scribe.Level.Error),
          doLogExceptions = (msg: String, ex: Throwable) => Async[F].delay(scribe.error(msg, ex)),
          noLog = Async[F].pure(()),
        )
        Async[F].fromCompletableFuture:
          val options = ArmeriaCatsServerOptions
            .customiseInterceptors(dispatcher)
            .corsInterceptor(Some {
              CORSInterceptor
                .customOrThrow[F](CORSConfig.default)
            })
            .serverLog(serverLog)
            .options
          
          val tapirService = ArmeriaCatsServerInterpreter[F](options)
            .toService(explorerEndpoints[F])
          val server = Server.builder
            .annotatedService(tapirService)
            .http(8081)
            .maxRequestLength(128 * 1024 * 1024)
            .requestTimeout(java.time.Duration.ofMinutes(2))
            .service(tapirService)
            .build
          Async[F].delay:
            scribe.info("server start / port: 8081")
            server.start().thenApply(_ => server)
    yield server

  override def run(args: List[String]): IO[ExitCode] =
    val program: Resource[IO, Server] =
      for server <- getServerResource[IO]
      yield server

    program.useForever.as(ExitCode.Success)
