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

    import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
    import sttp.tapir.openapi.circe.yaml.*

    val docs = OpenAPIDocsInterpreter().toOpenAPI(Api.postTxEndpoint, "LeisureMeta Chain", "0.1.0")
    println(docs.toYaml)

    IO(ExitCode.Success)
/*
    Dispatcher[IO]
      .flatMap { dispatcher =>
        Resource
          .make(
            IO.async_[Server] { cb =>
              val tapirService = ArmeriaCatsServerInterpreter[IO](dispatcher)
                .toService(Api.helloServerEndpoint)

              val server = Server
                .builder()
                .http(8080)
                .service(tapirService)
                .build()
              server.start().handle[Unit] {
                case (_, null)  => cb(Right(server))
                case (_, cause) => cb(Left(cause))
              }
            },
          )({ server =>
            IO.fromCompletableFuture(IO(server.closeAsync())).void
          })
      }
//      .use(_ => IO.never)
      .use { _ =>
        IO {
          val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

          def request(name: String, token: String) =
            val result = basicRequest
              .response(asStringAlways)
              .get(uri"http://localhost:8080/hello/world?name=$name")
              .auth
              .bearer(token)
              .send(backend)
            println(s"For $name and $token got body: ${result.body}, status code: ${result.code}")
            result

          assert(request("Papa Smurf", "secret123").body.contains("Hello, Papa Smurf (9)"))

          // by default, errors in the server logic correspond to status code 400
          assert(request("Gargamel", "secret123").body.contains("wrong name"))

          // will return the specified status code for authentication failures, 403
          assert(request("Papa Smurf", "hacker").body.contains("wrong token"))
        }
      }
      .as(ExitCode.Success)
*/