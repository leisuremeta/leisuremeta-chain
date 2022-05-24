package io.leisuremeta.chain
package node

import cats.effect.{Async, IO}
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import cats.syntax.apply.given
import cats.syntax.functor.given
import cats.syntax.flatMap.given

import com.linecorp.armeria.server.Server
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import api.{LeisureMetaChainApi as Api}
import api.model.PublicKeySummary
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.{
  LocalGossipService,
  LocalStatusService,
  NodeInitializationService,
  PeriodicActionService,
  TransactionService,
}
import service.interpreter.LocalGossipServiceInterpreter

final case class NodeApp[F[_]
  : Async: BlockRepository: StateRepository.AccountState.Name: StateRepository.AccountState.Key: TransactionRepository](
    config: NodeConfig,
):

  /** ****************************************************************************
    * Setup Endpoints
    * ****************************************************************************
    */

  import java.time.Instant
  import api.model.{Block, Signed, StateRoot}
  import lib.crypto.Hash
  import lib.datatype.{BigNat, UInt256}

  val nodeAddresses: IndexedSeq[PublicKeySummary] = config.wire.peers.map { peer =>
    PublicKeySummary
      .fromHex(peer.publicKeySummary)
      .getOrElse(
        throw new IllegalArgumentException(s"invalid pub key summary: ${peer.publicKeySummary}")
      )
  }
  val localKeyPair: KeyPair = {
    val privateKey = scala.sys.env
      .get("BBGO_PRIVATE_KEY")
      .map(BigInt(_, 16))
      .orElse(config.local.`private`).get
    CryptoOps.fromPrivate(privateKey)
  }

  val getLocalGossipService: F[LocalGossipService[F]] =

    val params = GossipDomain.GossipParams(
      nodeAddresses = nodeAddresses.zipWithIndex.map { case (address, i) =>
        i -> address
      }.toMap,
      timeWindowMillis = config.wire.timeWindowMillis,
      localKeyPair = localKeyPair,
    )
    scribe.debug(s"local gossip params: $params")
    LocalGossipServiceInterpreter
      .build[F](
        bestConfirmedBlock = NodeInitializationService.genesisBlock(config.genesis.timestamp),
        params = params,
      )

  def getStatusServerEndpoint = Api.getStatusEndpoint.serverLogic { _ =>
    LocalStatusService
      .status[F](
        networkId = config.local.networkId,
        genesisTimestamp = config.genesis.timestamp,
      )
      .value
      .flatMap {
        case Left(err)     => Async[F].raiseError(err)
        case Right(status) => Async[F].pure(Right(status))
      }
  }

  def postTxServerEndpoint(using LocalGossipService[F]) = Api.postTxEndpoint.serverLogic { (tx: Signed.Tx) =>
    scribe.info(s"receved postTx request: $tx")
    val result = TransactionService.submit[F](tx).value
    result.map{
      case Left(err) =>
        scribe.info(s"error occured in tx $tx: $err")
        Left(err)
      case Right(txHash) =>
        scribe.info(s"submitted tx: $txHash")
        Right(txHash)
    }
    result
  }

  def leisuremetaEndpoints(using LocalGossipService[F]) = List(
    getStatusServerEndpoint,
    postTxServerEndpoint,
  )

  val localPublicKeySummary: PublicKeySummary =
    PublicKeySummary.fromPublicKeyHash(localKeyPair.publicKey.toHash)

  val localNodeIndex: Int = config.wire.peers.map(_.publicKeySummary).indexOf(localPublicKeySummary)

  def periodicResource(using LocalGossipService[F]): Resource[F, F[Unit]] =
    PeriodicActionService.periodicAction[F](
      timeWindowMillis = config.wire.timeWindowMillis,
      numberOfNodes = config.wire.peers.size,
      localNodeIndex = localNodeIndex,
    )

  def getServer(dispatcher: Dispatcher[F])(using LocalGossipService[F]): F[Server] = for
    initializeResult <- NodeInitializationService
      .initialize[F](config.genesis.timestamp)
      .value
    _ <- initializeResult match
      case Left(err) => Async[F].raiseError(Exception(err))
      case Right(_)  => Async[F].unit
    server <- Async[F].async_[Server] { cb =>
      val tapirService = ArmeriaCatsServerInterpreter[F](dispatcher)
        .toService(leisuremetaEndpoints)
      val server = Server.builder
        .http(config.local.port.value)
        .service(tapirService)
        .build
      server.start.handle[Unit] {
        case (_, null)  => cb(Right(server))
        case (_, cause) => cb(Left(cause))
      }
    }
  yield server

  def resource: F[Resource[F, Server]] = for
    localGossipService <- getLocalGossipService
  yield
    given LocalGossipService[F] = localGossipService
    for
      _ <- periodicResource
      dispatcher <- Dispatcher[F]
      server <- Resource.make(getServer(dispatcher))(server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ()),
      )
    yield server
