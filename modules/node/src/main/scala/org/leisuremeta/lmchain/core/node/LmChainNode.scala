package org.leisuremeta.lmchain.core
package node

import java.nio.file.{Path, Paths}

import scala.concurrent.ExecutionContext

import cats.effect.{ExitCode, IO, IOApp, Resource}

import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.http.filter.Cors
import com.twitter.util.Future
import eu.timepit.refined.pureconfig._
import eu.timepit.refined.types.net.PortNumber
import io.circe.generic.auto._
import io.circe.refined._
import io.finch._
import io.finch.circe._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

import codec.byte.ByteCodec
import codec.circe._
import crypto.{CryptoOps, Hash, KeyPair}
import crypto.Hash.ops._
import datatype._
import gossip.GossipClient
import gossip.thrift.{ThriftGossipClient, ThriftGossipServer}
import node.endpoint._
import model._
import model.Transaction.Token.DefinitionId
import repository._
import service._
import service.interpreter._
import store._
import store.interpreter.StoreIndexSwayInterpreter

object LmChainNode
    extends IOApp
    with Endpoint.Module[IO]
    with ConfigurationSupport {

  /** ****************************************************************************
    * Load Config
    * ****************************************************************************
    */
  case class NodeConfig(networkId: NetworkId, port: PortNumber)
  case class WireConfig(
      timeWindowMillis: Long,
      localPrivate: String,
      port: PortNumber,
      peers: IndexedSeq[PeerConfig],
  )
  case class PeerConfig(dest: String, address: String)
  case class Config(node: NodeConfig, wire: WireConfig)

  val configEither: Either[ConfigReaderFailures, Config] =
    pureconfig.ConfigSource.default.load[Config]
  scribe.debug(s"load config: $configEither")
  @SuppressWarnings(Array("org.wartremover.warts.PublicInference"))
  val Right(Config(nodeConfig, wireConfig)) = configEither

  /** ****************************************************************************
    * Setup Repositories
    * ****************************************************************************
    */
  implicit val ec: ExecutionContext = ExecutionContext.global

  def sway[K: ByteCodec, V: ByteCodec](dir: Path): StoreIndex[IO, K, V] =
    StoreIndexSwayInterpreter[K, V](dir)

  implicit val bestBlockHeaderStore: SingleValueStore[IO, Block.Header] =
    SingleValueStore.fromKeyValueStore(
      sway[UInt256Bytes, Block.Header](Paths.get("sway", "block", "best"))
    )

  implicit val blockhashStoreIndex: StoreIndex[IO, Hash.Value[Block], Block] =
    sway[Hash.Value[Block], Block](Paths.get("sway", "block"))

  implicit val blockNumberIndex: StoreIndex[IO, BigNat, Block.BlockHash] =
    sway[BigNat, Block.BlockHash](Paths.get("sway", "block", "number"))

  implicit val txBlockIndex: KeyValueStore[IO, Signed.TxHash, Block.BlockHash] =
    sway[Signed.TxHash, Block.BlockHash](Paths.get("sway", "block", "tx"))

  implicit val txStore: StoreIndex[IO, Hash.Value[Signed.Tx], Signed.Tx] =
    sway[Hash.Value[Signed.Tx], Signed.Tx](Paths.get("sway", "transaction"))

  implicit val blockRepo: BlockRepository[IO] = BlockRepository.fromStores[IO]
  implicit val txRepo: TransactionRepository[IO] =
    TransactionRepository.fromStores[IO]
  implicit val nameStateRepo: StateRepository[IO, Account.Name, NameState] =
    StateRepository.fromStores[IO, Account.Name, NameState](
      cats.Monad[IO],
      sway(Paths.get("sway", "name")),
    )
  implicit val tokenStateRepo: StateRepository[IO, DefinitionId, TokenState] =
    StateRepository.fromStores[IO, DefinitionId, TokenState](
      cats.Monad[IO],
      sway(Paths.get("sway", "token")),
    )
  implicit val balanceStateRepo
      : StateRepository[IO, (Account, Transaction.Input.Tx), Unit] =
    StateRepository.fromStores[IO, (Account, Transaction.Input.Tx), Unit](
      cats.Monad[IO],
      sway(Paths.get("sway", "balance")),
    )

  /** ****************************************************************************
    * Setup Gossip
    * ****************************************************************************
    */
  val nodeAddresses: IndexedSeq[Address] = wireConfig.peers.map { peer =>
    Address
      .fromHex(peer.address)
      .getOrElse(
        throw new IllegalArgumentException(s"invalid address: ${peer.address}")
      )
  }

  val localKeyPair: KeyPair = {
    val localPrivate = scala.sys.env
      .get("BBGO_PRIVATE_KEY")
      .getOrElse(wireConfig.localPrivate)
    val privateKey = BigInt(localPrivate, 16)
    CryptoOps.fromPrivate(privateKey)
  }

  implicit val localGossipService: LocalGossipService[IO] = {
    val params = GossipDomain.GossipParams(
      nodeAddresses = nodeAddresses.zipWithIndex.map { case (address, i) =>
        i -> address
      }.toMap,
      timeWindowMillis = wireConfig.timeWindowMillis,
      localKeyPair = localKeyPair,
    )
    scribe.debug(s"local gossip params: $params")
    LocalGossipServiceInterpreter
      .build[IO](
        bestConfirmedBlock = NodeInitializationService.GenesisBlock,
        params = params,
      )
      .unsafeRunSync()
  }

  val localAddress: Address =
    Address.fromPublicKeyHash(localKeyPair.publicKey.toHash)

  val localNodeIndex: Int = nodeAddresses.indexOf(localAddress)

  val peers: Map[Int, GossipClient[IO]] = (for {
    (peer, i) <- wireConfig.peers.zipWithIndex if i != localNodeIndex
  } yield i -> new ThriftGossipClient[IO](peer.dest)).toMap

  /** ****************************************************************************
    * Setup Endpoints and API
    * ****************************************************************************
    */

  implicit val finch: EndpointModule[IO] = io.finch.catsEffect

  implicit def txHashEncoder: io.circe.Encoder[Hash.Value[Transaction]] =
    codec.circe.taggedEncoder[UInt256Bytes, Transaction]

  private val jsonEndpoint = (
    NodeStatusEndpoint.Get[IO](
      nodeConfig.networkId,
      NodeInitializationService.GenesisBlock.toHash,
    )
      :+: BlockEndpoint.Get[IO]
      :+: StateEndpoint.GetName[IO]
      :+: StateEndpoint.GetToken[IO]
      :+: StateEndpoint.GetBalance[IO]
      :+: TxHashEndpoint.Post[IO]
      :+: TransactionEndpoint.Get[IO]
      :+: TransactionEndpoint.Post[IO])

  val rootRedirect: Endpoint[IO, Unit] = get(pathEmpty) {
    Output.unit(Status.SeeOther).withHeader("Location" -> "/index.html")
  }

  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some("*"),
    allowsMethods = _ => Some(Seq("GET", "OPTION", "POST")),
    allowsHeaders = _ => Some(Seq("Accept", "Content-Type")),
  )

  val api: Service[Request, Response] = new Cors.HttpFilter(policy).andThen {
    Bootstrap
      .configure(enableMethodNotAllowed = true)
      .serve[Text.Html](rootRedirect)
      .serve[Text.Html](classpathAsset("/index.html"))
      .serve[Application.Javascript](classpathAsset("/lmchain.js"))
      .serve[Application.Javascript](
        classpathAsset("/client-fastopt-bundle.js")
      )
      .serve[Application.Javascript](
        classpathAsset("/client-fastopt-bundle.js.map")
      )
      .serve[Application.Json](jsonEndpoint)
      .toService
  }

  def getServer(): ListeningServer = Http.server
    .withStreaming(enabled = true)
    .withAdmissionControl
    .concurrencyLimit(maxConcurrentRequests = 10, maxWaiters = 10)
    .serve(s":${nodeConfig.port}", api)

  def getGossipServer(): ListeningServer = ThriftGossipServer.serve(
    s":${wireConfig.port}"
  )(PeerGossipService.gossipApi[IO])

  val serverResource: Resource[IO, ListeningServer] = Resource.make {
    IO(getServer())
  } { server =>
    IO.defer(implicitly[ToAsync[Future, IO]].apply(server.close()))
  }

  val gossipServerResource: Resource[IO, ListeningServer] = Resource.make {
    IO(getGossipServer())
  } { server =>
    IO.defer(implicitly[ToAsync[Future, IO]].apply(server.close()))
  }

  val periodicResource: Resource[IO, IO[Unit]] =
    PeriodicActionService.periodicAction[IO](
      timeWindowMillis = wireConfig.timeWindowMillis,
      numberOfNodes = wireConfig.peers.size,
      localNodeIndex = localNodeIndex,
      gossipClients = peers,
    )
    
  val ancestorBlockPullingResource: Resource[IO, IO[Unit]] =
    NodeInitializationService.pullAncestorBlocksByLocalGossip[IO](
      pullingPeriodMillis = wireConfig.timeWindowMillis,
      gossipClients = peers,
    ).background

  val allResource: Resource[IO, Unit] = for {
    _ <- serverResource
    _ <- gossipServerResource
    _ <- periodicResource
    _ <- ancestorBlockPullingResource
  } yield ()

  def run(args: List[String]): IO[ExitCode] = for {
    result <- NodeInitializationService
      .initialize[IO](peers, localNodeIndex)
      .value
    _        <- IO.delay { scribe.debug(s"Node initialization result: $result") }
    exitCode <- allResource.use(_ => IO.never).as(ExitCode.Success)
  } yield exitCode
}
