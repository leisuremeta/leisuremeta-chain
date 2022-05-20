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
import repository.BlockRepository
import service.{LocalStatusService, NodeInitializationService, TransactionService}

final case class NodeApp[F[_]: Async: BlockRepository](config: NodeConfig):

  /** ****************************************************************************
    * Setup Endpoints
    * ****************************************************************************
    */

  import java.time.Instant
  import api.model.{Block, Signed, StateRoot}
  import lib.crypto.Hash
  import lib.datatype.{BigNat, UInt256}

  val getStatusServerEndpoint = Api.getStatusEndpoint.serverLogic { _ =>
    import lib.crypto.Hash.ops.*
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

  val leisuremetaEndpoints = List(
    getStatusServerEndpoint,
  )

  def getServer(dispatcher: Dispatcher[F]): F[Server] = for
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

  def resource: Resource[F, Server] = for
    dispatcher <- Dispatcher[F]
    server <- Resource.make(getServer(dispatcher))(server =>
      Async[F]
        .fromCompletableFuture(Async[F].delay(server.closeAsync()))
        .map(_ => ()),
    )
  yield server
