package io.leisuremeta.chain
package node

import scala.jdk.CollectionConverters.*
import scala.util.Try
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.traverse.given
import com.typesafe.config.{Config, ConfigException}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.types.net.PortNumber
import eu.timepit.refined.refineV
import api.model.NetworkId
import lib.datatype.{BigNat, UInt256, UInt256BigInt}
import NodeConfig.*
import java.time.Instant

final case class NodeConfig(
    local: LocalConfig,
    wire: WireConfig,
    genesis: GenesisConfig,
)

object NodeConfig:
  def load[F[_]: Async](getConf: F[Config]): EitherT[F, String, NodeConfig] = for
    config  <- EitherT.right[String](getConf)
    local   <- EitherT.fromEither[F](LocalConfig.load(config))
    wire    <- EitherT.fromEither[F](WireConfig.load(config))
    genesis <- EitherT.fromEither[F](GenesisConfig.load(config))
  yield NodeConfig(local, wire, genesis)

  case class LocalConfig(
      networkId: NetworkId,
      port: PortNumber,
      `private`: UInt256BigInt,
  ):
    override def toString: String = s"LocalConfig($networkId, $port, **hidden**)"

  object LocalConfig:
    def load(config: Config): Either[String, LocalConfig] = for
      networkIdLong <- either(config.getLong("local.network-id"))
      networkId     <- BigNat.fromBigInt(BigInt(networkIdLong))
      portInt       <- either(config.getInt("local.port"))
      port          <- refineV[Interval.Closed[0, 65535]](portInt)
      privString    <- either(config.getString("local.private"))
      priv          <- UInt256.from(BigInt(privString, 16)).left.map(_.msg)
    yield LocalConfig(NetworkId(networkId), port, priv)

  case class WireConfig(
      timeWindowMillis: Long,
      port: PortNumber,
      peers: IndexedSeq[PeerConfig],
  )
  object WireConfig:
    def load(config: Config): Either[String, WireConfig] = for
      timeWindowMillis <- either(config.getLong("wire.time-window-millis"))
      portInt          <- either(config.getInt("wire.port"))
      port             <- refineV[Interval.Closed[0, 65535]](portInt)
      peers            <- either(config.getConfigList("wire.peers"))
      peerConfigs      <- peers.asScala.toVector.traverse(PeerConfig.load)
    yield WireConfig(timeWindowMillis, port, peerConfigs)

  case class PeerConfig(dest: String, publicKeySummary: String)
  object PeerConfig:
    def load(config: Config): Either[String, PeerConfig] = for
      dest    <- either(config.getString("dest"))
      publicKeySummary <- either(config.getString("public-key-summary"))
    yield PeerConfig(dest, publicKeySummary)

  case class GenesisConfig(timestamp: Instant)
  object GenesisConfig:
    def load(config: Config): Either[String, GenesisConfig] = for
      timestampString <- either(config.getString("genesis.timestamp"))
      timestamp       <- either(Instant.parse(timestampString))
    yield GenesisConfig(timestamp)

  private def either[A](action: => A): Either[String, A] =
    Try(action).toEither.left.map {
      case e: ConfigException.Missing   => s"Missing config: ${e.getMessage}"
      case e: ConfigException.WrongType => s"Wrong type: ${e.getMessage}"
      case e: Exception                 => s"Exception: ${e.getMessage}"
    }
