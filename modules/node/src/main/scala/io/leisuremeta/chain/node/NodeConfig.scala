package io.leisuremeta.chain
package node

import scala.jdk.CollectionConverters.*
import scala.util.Try
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.traverse.given
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.types.net.PortNumber
import eu.timepit.refined.refineV
import api.model.NetworkId
import lib.datatype.{BigNat, UInt256, UInt256BigInt}
import NodeConfig.*

final case class NodeConfig(
    local: LocalConfig,
    wire: WireConfig,
)

object NodeConfig:
  def load[F[_]: Async]: EitherT[F, String, NodeConfig] = for
    config <- EitherT.right[String](Async[F].blocking(ConfigFactory.load))
    local  <- EitherT.fromEither[F](LocalConfig.load(config))
    wire   <- EitherT.fromEither[F](WireConfig.load(config))
  yield NodeConfig(local, wire)

  case class LocalConfig(
      networkId: NetworkId,
      port: PortNumber,
      `private`: UInt256BigInt,
  )
  object LocalConfig:
    def load(config: Config): Either[String, LocalConfig] = for
      networkIdLong <- either(config.getLong("local.network-id"))
      networkId     <- BigNat.fromBigInt(BigInt(networkIdLong))
      portInt       <- either(config.getInt("local.port"))
      port          <- refineV[Interval.Closed[0, 65535]](portInt)
      privString    <- either(config.getString("local.private"))
      priv          <- UInt256.from(BigInt(privString)).left.map(_.msg)
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

  case class PeerConfig(dest: String, address: String)
  object PeerConfig:
    def load(config: Config): Either[String, PeerConfig] = for
      dest    <- either(config.getString("dest"))
      address <- either(config.getString("paddress"))
    yield PeerConfig(dest, address)

  private def either[A](action: => A): Either[String, A] =
    Try(action).toEither.left.map {
      case e: ConfigException.Missing   => s"Missing config: ${e.getMessage}"
      case e: ConfigException.WrongType => s"Wrong type: ${e.getMessage}"
      case e: Exception                 => s"Exception: ${e.getMessage}"
    }
