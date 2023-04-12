package io.leisuremeta.chain.node
package proxy

import io.circe.generic.auto.*
import io.circe.parser.decode
import scala.util.Try
import scala.concurrent.duration.*
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.Ref
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.*
import cats.syntax.*
import cats.syntax.all._
import scala.jdk.CollectionConverters.*
import com.linecorp.armeria.client.ClientFactory
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.client.encoding.DecodingClient
import cats.effect.kernel.Async
import cats.syntax.flatMap.toFlatMapOps
import service.*
import java.nio.file.{Files, Paths, StandardOpenOption}
import model.NodeConfig

object NodeProxyMain extends IOApp:
  def newNodeWatchLoop[F[_]: Async](apiService: InternalApiService[F], blocker: Ref[F, Boolean], blcUrls: Ref[F, List[String]]): F[Unit] = Async[F].blocking {
    val path = Paths.get("migration-node.json")
    for 
      json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
      nodeConfig <- decode[NodeConfig](json)
    yield nodeConfig
  }.flatMap { nodeConfigEither =>
    nodeConfigEither match 
      case Right(nodeConfig) => 
        nodeConfig.newNodeAddress match 
          case Some(newNodeAddress) => 
            blcUrls.getAndUpdate { urls =>
              if urls.contains(newNodeAddress) then urls 
              else {
                blocker.set(true)
                NodeBalancer(apiService, blocker, nodeConfig).run()
                urls :+ newNodeAddress
              }
            }
            ()
          case None => ()
      case Left(error) =>
        scribe.error(s"Error decoding Node Config: $error")

    Async[F].sleep(5.seconds) >> newNodeWatchLoop(apiService, blocker, blcUrls)
  } 

  def newClientFactory(options: SttpBackendOptions): ClientFactory =
    val builder = ClientFactory
      .builder()
      .connectTimeoutMillis(options.connectionTimeout.toMillis)
    options.proxy.fold(builder.build()) { proxy =>
      builder
        .proxyConfig(proxy.asJavaProxySelector)
        .build()
    }

  def webClient(options: SttpBackendOptions): WebClient = 
    WebClient
      .builder()
      .decorator(
        DecodingClient
          .builder()
          .autoFillAcceptEncoding(false)
          .strictContentEncoding(false)
          .newDecorator()
      )
      .factory(newClientFactory(options))
      .build()

  def run[F[_]: Async]: F[ExitCode] =
    ArmeriaCatsBackend
      .resourceUsingClient[F](webClient(SttpBackendOptions.Default))
      .use { backend =>
        for 
          blocker        <- Ref.of[F, Boolean](false)
          blockchainUrls <- Ref.of[F, List[String]](List(""))
          internalApiService <- InternalApiService[F](backend, blocker, blockchainUrls)
          _              <- newNodeWatchLoop(internalApiService, blocker, blockchainUrls)
          appResource    <- NodeProxyApp[F](
                              internalApiService
                            ).resource
          exitcode <- appResource.useForever.as(ExitCode.Success)
        yield exitcode
      }
  override def run(args: List[String]): IO[ExitCode] = {
    run[IO]
  }
    
  