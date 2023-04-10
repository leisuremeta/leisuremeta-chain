package io.leisuremeta.chain.node
package proxy

import util.GlobalFlag
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
import service.InternalApiService

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
          blocker <- Ref.of[F, Boolean](false)
          appResource <- NodeProxyApp[F](
                           InternalApiService[F](backend, blocker)
                         ).resource
          exitcode <- appResource.useForever.as(ExitCode.Success)
        yield exitcode

      }
  override def run(args: List[String]): IO[ExitCode] = {
    run[IO]
  }
    
  