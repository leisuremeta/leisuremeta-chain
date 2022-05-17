package io.leisuremeta.chain
package node

import cats.effect.{Async, IO}
import cats.effect.std.Dispatcher
import cats.effect.kernel.Resource
import cats.syntax.functor.given

import com.linecorp.armeria.server.Server
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import api.{LeisureMetaChainApi as Api}

final case class NodeApp[F[_]: Async](config: NodeConfig):

  val getStatusServerEndpoint = Api.getStatusEndpoint.serverLogic { _ =>
    Async[F].delay(Right("Ok"))
  }

  val leisuremetaEndpoints = List(
    getStatusServerEndpoint,
  )

  def resource: Resource[F, Server] = for
    dispatcher <- Dispatcher[F]
    server <- Resource.make(Async[F].async_[Server] { cb =>
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
    })(server =>
      Async[F]
        .fromCompletableFuture(Async[F].delay(server.closeAsync()))
        .map(_ => ()),
    )
  yield server
