package io.leisuremeta.chain.node
package proxy

import cats.effect.std.{Dispatcher, Semaphore}
import cats.effect.{Async, IO}
import cats.effect.kernel.Resource
import com.linecorp.armeria.server.Server
import cats.syntax.apply.given
import cats.syntax.flatMap.given
import cats.syntax.functor.given

import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.ServerEndpoint
import sttp.capabilities.fs2.Fs2Streams

import io.leisuremeta.chain.node.proxy.{NodeProxyApi as Api}
import io.leisuremeta.chain.api.LeisureMetaChainApi.ServerError
import io.leisuremeta.chain.api.model.account.EthAddress
import io.leisuremeta.chain.api.model.*
import io.leisuremeta.chain.api.model.api_model.{AccountInfo, BalanceInfo}
import io.leisuremeta.chain.api.model.token.*
import service.InternalApiService


final case class NodeProxyApp[F[_]: Async](
   apiService: InternalApiService[F]
):
  
  def getBlockServerEndpoint = Api.getBlockEndpoint.serverLogic {
    (blockHash: String) =>
      apiService.getBlock(blockHash).map(Right(_))
  }

  def getAccountServerEndpoint: ServerEndpoint[Fs2Streams[F], F] = Api.getAccountEndpoint.serverLogic {
    (a: Account) =>
      apiService.getAccount(a).map(Right(_))
  }
  
  def getEthServerEndpoint = Api.getEthEndpoint.serverLogic {
    (ethAddress: EthAddress) =>
      apiService.getEthAccount(ethAddress).map(Right(_))
  }
  
  def getGroupServerEndpoint = Api.getGroupEndpoint.serverLogic {
    (g: GroupId) =>
      apiService.getGroupInfo(g).map(Right(_))
  }
  
  def getBlockListServerEndpoint = Api.getBlockListEndpoint.serverLogic {
    (fromOption, limitOption) =>
      apiService.getBlockList(fromOption, limitOption).map(Right(_))
  }

  def getStatusServerEndpoint = Api.getStatusEndpoint.serverLogic { _ =>
    apiService.getStatus.map(Right(_))
  }

  def getTokenDefServerEndpoint = Api.getTokenDefinitionEndpoint.serverLogic {
    (tokenDefinitionId: TokenDefinitionId) =>
      apiService.getTokenDef(tokenDefinitionId).map(Right(_))
  }

  def getBalanceServerEndpoint = Api.getBalanceEndpoint.serverLogic {
    (account, movable) =>
      apiService.getBalance(account, movable).map(Right(_))
  }
    
  def getNftBalanceServerEndpoint = Api.getNftBalanceEndpoint.serverLogic {
    (account, movable) =>
      apiService.getNftBalance(account, movable).map(Right(_))
  }
  
  def getTokenServerEndpoint = Api.getTokenEndpoint.serverLogic {
    (tokenId: TokenId) =>
      apiService.getToken(tokenId).map(Right(_))
  }


  def getOwnersServerEndpoint = Api.getOwnersEndpoint.serverLogic {
    (tokenDefinitionId: TokenDefinitionId) =>
      apiService.getOwners(tokenDefinitionId).map(Right(_))
  }
  

  def getAccountActivityServerEndpoint =
    Api.getAccountActivityEndpoint.serverLogic { (account: Account) =>
      apiService.getAccountActivity(account).map(Right(_))
    }

  def getTokenActivityServerEndpoint =
    Api.getTokenActivityEndpoint.serverLogic { (tokenId: TokenId) =>
      apiService.getTokenActivity(tokenId).map(Right(_))
    }

  def getAccountSnapshotServerEndpoint =
    Api.getAccountSnapshotEndpoint.serverLogic { (account: Account) =>
      apiService.getAccountSnapshot(account).map(Right(_))
    }

  def getTokenSnapshotServerEndpoint =
    Api.getTokenSnapshotEndpoint.serverLogic { (tokenId: TokenId) =>
      apiService.getTokenSnapshot(tokenId).map(Right(_))
        
    }

  def getOwnershipSnapshotServerEndpoint =
    Api.getOwnershipSnapshotEndpoint.serverLogic { (tokenId: TokenId) =>
      apiService.getOwnershipSnapshot(tokenId).map(Right(_))
    }

  def getOwnershipSnapshotMapServerEndpoint =
    Api.getOwnershipSnapshotMapEndpoint.serverLogic{
      (from: Option[String], limit: Option[Int]) =>
        apiService.getOwnershipSnapshotMap(from, limit).map(Right(_))
    }
    
  def getOwnershipRewardedServerEndpoint =
    Api.getOwnershipRewardedEndpoint.serverLogic { (tokenId: TokenId) =>
      apiService.getOwnershipRewarded(tokenId).map(Right(_))
    }

  def postTxEndpoint =
    Api.postTxEndpoint.serverLogic { (txs: String) =>
      apiService.postTx(txs).map(Right(_))
    }

  def postTxHashEndpoint = Api.postTxHashEndpoint.serverLogic {
    (txs: String) =>
      apiService.postTxHash(txs).map(Right(_))
    }

  def getTxServerEndpoint = Api.getTxEndpoint.serverLogic {
    (txHash: String) =>
      apiService.getTx(txHash).map(Right(_))
    }

  def getTxSetServerEndpoint = Api.getTxSetEndpoint.serverLogic {
    (block: String) =>
      apiService.getTxSet(block).map(Right(_))
    }

  def postTxHashServerEndpoint =
    Api.postTxHashEndpoint.serverLogic { 
      (txs: String) =>
        apiService.postTxHash(txs).map(Right(_))
    }

  def postTxServerEndpoint =
    Api.postTxEndpoint.serverLogic {
      scribe.info("postTx 생성요청")
      (txs: String) =>
        apiService.postTx(txs).map(Right(_))
    }

  def proxyNodeEndpoints = List(
    getAccountServerEndpoint,
    getEthServerEndpoint,
    getBlockListServerEndpoint,
    getGroupServerEndpoint,
    getBlockServerEndpoint,
    getStatusServerEndpoint,
    getTokenDefServerEndpoint,
    getBalanceServerEndpoint,
    getNftBalanceServerEndpoint,
    getTokenServerEndpoint,
    getOwnersServerEndpoint,
    getTokenActivityServerEndpoint,
    getAccountSnapshotServerEndpoint,
    getTokenSnapshotServerEndpoint,
    getTxSetServerEndpoint,
    getAccountActivityServerEndpoint,
    getTxServerEndpoint,
    getOwnershipSnapshotServerEndpoint,
    getOwnershipSnapshotMapServerEndpoint,
//    getRewardServerEndpoint,
    getOwnershipRewardedServerEndpoint,
    postTxServerEndpoint,
    postTxHashServerEndpoint,
  )

  def getServer[IO](
    dispatcher: Dispatcher[F],
  ): F[Server] = for 
    server <- Async[F].async_[Server] { cb =>
      def log[F[_]: Async](
          level: scribe.Level,
        )(msg: String, exOpt: Option[Throwable])(using
          mdc: scribe.data.MDC,
        ): F[Unit] =
          Async[F].delay(exOpt match
            case None     => scribe.log(level, mdc, msg)
            case Some(ex) => scribe.log(level, mdc, msg, ex),
        )
      val serverLog = DefaultServerLog(
        doLogWhenReceived = log(scribe.Level.Info)(_, None),
        doLogWhenHandled  = log(scribe.Level.Info),
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
        .toService(proxyNodeEndpoints)
      val server = Server.builder
        .maxRequestLength(128 * 1024 * 1024)
        .requestTimeout(java.time.Duration.ofMinutes(10))
        .http(8080)
        .service(tapirService)
        .build
      server.start.handle[Unit] {
        case (_, null)  => cb(Right(server))
        case (_, cause) => cb(Left(cause))
      }
    }
  yield server

  def resource: F[Resource[F, Server]] = Async[F].delay {
    for 
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(getServer(dispatcher))(server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ())
      )
    yield server
  }

