package io.leisuremeta.chain
package node

import cats.effect.{Async, IO}
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import cats.syntax.apply.given
import cats.syntax.functor.given
import cats.syntax.flatMap.given

import com.linecorp.armeria.server.Server
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import api.{LeisureMetaChainApi as Api}
import api.model.{Account, Block, GroupId, PublicKeySummary, Transaction}
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.{
  LocalGossipService,
  LocalStatusService,
  NodeInitializationService,
  PeriodicActionService,
  StateReadService,
  TransactionService,
}
import service.interpreter.LocalGossipServiceInterpreter
import io.leisuremeta.chain.node.service.BlockService

final case class NodeApp[F[_]
  : Async: BlockRepository: StateRepository.AccountState: StateRepository.GroupState: TransactionRepository](
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

  val nodeAddresses: IndexedSeq[PublicKeySummary] = config.wire.peers.map {
    peer =>
      PublicKeySummary
        .fromHex(peer.publicKeySummary)
        .getOrElse(
          throw new IllegalArgumentException(
            s"invalid pub key summary: ${peer.publicKeySummary}",
          ),
        )
  }
  val localKeyPair: KeyPair =
    val privateKey = scala.sys.env
      .get("BBGO_PRIVATE_KEY")
      .map(BigInt(_, 16))
      .orElse(config.local.`private`)
      .get
    CryptoOps.fromPrivate(privateKey)

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
        bestConfirmedBlock =
          NodeInitializationService.genesisBlock(config.genesis.timestamp),
        params = params,
      )

  def getAccountServerEndpoint = Api.getAccountEndpoint.serverLogic {
    (a: Account) =>
      StateReadService.getAccountInfo(a).map {
        case Some(info) => Right(info)
        case None       => Left(Right(Api.NotFound(s"account not found: $a")))
      }
  }

  def getGroupServerEndpoint = Api.getGroupEndpoint.serverLogic {
    (g: GroupId) =>
      StateReadService.getGroupInfo(g).map {
        case Some(info) => Right(info)
        case None       => Left(Right(Api.NotFound(s"group not found: $g")))
      }
  }

  def getBlockServerEndpoint = Api.getBlockEndpoint.serverLogic {
    (blockHash: Block.BlockHash) =>
      val result = BlockService.get(blockHash).value
      
      result.map {
        case Right(Some(block)) => Right(block)
        case Right(None) => Left(Right(Api.NotFound(s"block not found: $blockHash")))
        case Left(err)   => Left(Left(Api.ServerError(err)))
      }
  }

  def getStatusServerEndpoint = Api.getStatusEndpoint.serverLogicSuccess { _ =>
    LocalStatusService
      .status[F](
        networkId = config.local.networkId,
        genesisTimestamp = config.genesis.timestamp,
      )
  }

  def postTxServerEndpoint(using LocalGossipService[F]) =
    Api.postTxEndpoint.serverLogic { (txs: Seq[Signed.Tx]) =>
      scribe.info(s"received postTx request: $txs")
      val result = TransactionService.submit[F](txs).value
      result.map {
        case Left(err) =>
          scribe.info(s"error occured in tx $txs: $err")
          Left(Right(Api.BadRequest(err)))
        case Right(txHashes) =>
          scribe.info(s"submitted txs: $txHashes")
          Right(txHashes)
      }
    }

  def getTxServerEndpoint = Api.getTxEndpoint.serverLogic {
    (txHash: Signed.TxHash) =>
      scribe.info(s"received getTx request: $txHash")
      val result = TransactionService.get(txHash).value

      result.map {
        case Left(err) =>
          scribe.info(s"error occured in getting tx $txHash: $err")
        case Right(tx) =>
          scribe.info(s"got tx: $tx")
      }

      result.map {
        case Right(Some(tx)) => Right(tx)
        case Right(None) => Left(Right(Api.NotFound(s"tx not found: $txHash")))
        case Left(err)   => Left(Left(Api.ServerError(err)))
      }
  }

  def postTxHashServerEndpoint(using LocalGossipService[F]) =
    Api.postTxHashEndpoint.serverLogicPure[F] { (txs: Seq[Transaction]) =>
      scribe.info(s"received postTxHash request: $txs")
      Right(txs.map(_.toHash))
    }

  def leisuremetaEndpoints(using
      LocalGossipService[F],
  ): List[ServerEndpoint[Fs2Streams[F], F]] = List(
    getAccountServerEndpoint,
    getBlockServerEndpoint,
    getGroupServerEndpoint,
    getStatusServerEndpoint,
    getTxServerEndpoint,
    postTxServerEndpoint,
    postTxHashServerEndpoint,
  )

  val localPublicKeySummary: PublicKeySummary =
    PublicKeySummary.fromPublicKeyHash(localKeyPair.publicKey.toHash)

  val localNodeIndex: Int =
    config.wire.peers.map(_.publicKeySummary).indexOf(localPublicKeySummary)

//  def periodicResource(using LocalGossipService[F]): Resource[F, F[Unit]] =
//    PeriodicActionService.periodicAction[F](
//      timeWindowMillis = config.wire.timeWindowMillis,
//      numberOfNodes = config.wire.peers.size,
//      localNodeIndex = localNodeIndex,
//    )

  def getServer(
      dispatcher: Dispatcher[F],
  )(using LocalGossipService[F]): F[Server] = for
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

  def resource: F[Resource[F, Server]] = for localGossipService <-
      getLocalGossipService
  yield
    given LocalGossipService[F] = localGossipService
    for
//      _ <- periodicResource
      dispatcher <- Dispatcher[F]
      server <- Resource.make(getServer(dispatcher))(server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ()),
      )
    yield server
