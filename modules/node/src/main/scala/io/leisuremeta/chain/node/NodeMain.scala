package io.leisuremeta.chain
package node

import cats.effect.*
import cats.effect.std.Dispatcher

import com.linecorp.armeria.server.Server
import sttp.client3.*
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.armeria.cats.ArmeriaCatsServerInterpreter

import api.{LeisureMetaChainApi as Api}

object NodeMain extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    NodeConfig.load[IO].value.flatMap{
      case Right(config) =>
        NodeApp[IO](config).resource.useForever.as(ExitCode.Success)
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
