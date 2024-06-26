package io.leisuremeta.chain.node
package proxy

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.Ref
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.*
import cats.syntax.*
import cats.syntax.all._
import com.linecorp.armeria.client.ClientFactory
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.client.encoding.DecodingClient
import cats.effect.kernel.Async
import cats.syntax.flatMap.toFlatMapOps
import model.NodeConfig
import service.*

object NodeProxyMain extends IOApp:
  
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
          blocker        <- Ref.of[F, Boolean](true)
          queue          <- PostTxQueue[F]
          nodeConfg      <- NodeWatchService.nodeConfig.flatMap (
                              _.fold(Async[F].raiseError[NodeConfig], Async[F].pure))
          blockchainUrls <- Ref.of[F, List[String]](List(nodeConfg.oldNodeAddress))
          internalApiSvc =  InternalApiService[F](backend, blocker, blockchainUrls, queue)
          _              <- NodeWatchService.startOnNew(internalApiSvc, blockchainUrls, blocker, queue) 
          appResource    <- NodeProxyApp[F](internalApiSvc).resource
          exitcode       <- appResource.useForever.as(ExitCode.Success)
        yield exitcode
      }
  override def run(args: List[String]): IO[ExitCode] = {
    run[IO]
  }
  

