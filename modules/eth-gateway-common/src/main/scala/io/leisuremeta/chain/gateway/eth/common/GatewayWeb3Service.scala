package io.leisuremeta.chain.gateway.eth.common

import cats.effect.{Async, Resource}

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object GatewayWeb3Service:
  def web3Resource[F[_]: Async](url: String): Resource[F, Web3j] = Resource.make {

    val interceptor = HttpLoggingInterceptor()
    interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)

    val client = OkHttpClient
      .Builder()
      .addInterceptor(interceptor)
      .build()

    Async[F].delay(Web3j.build(new HttpService(url, client)))
  }(web3j => Async[F].delay(web3j.shutdown()))
