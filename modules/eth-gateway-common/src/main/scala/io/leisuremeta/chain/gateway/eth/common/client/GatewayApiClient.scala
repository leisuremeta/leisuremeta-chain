package io.leisuremeta.chain.gateway.eth.common
package client

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.bifunctor.*
import cats.syntax.functor.*

import sttp.client3.SttpBackend
import sttp.model.Uri
import sttp.tapir.DecodeResult
import sttp.tapir.client.sttp.SttpClientInterpreter

trait GatewayApiClient[F[_]]:
  def get(
      key: String,
      doublyEncryptedFrontPartBase64: String,
  ): EitherT[F, String, String]

object GatewayApiClient:

  def apply[F[_]: GatewayApiClient]: GatewayApiClient[F] = summon

  def make[F[_]: Async](
    backend: SttpBackend[F, Any],
    uri: Uri
  ): GatewayApiClient[F] =
    val sttpClient = SttpClientInterpreter().toClient(
      GatewayApi.postDecryptEndpoint,
      Some(uri),
      backend,
    )
    def sanitize[A, B](
        result: DecodeResult[Either[A, B]],
    ): Either[String, B] =
      result match
        case DecodeResult.Value(v)   => v.leftMap(_.toString)
        case f: DecodeResult.Failure => Left(s"Fail: ${f.toString()}")

    new GatewayApiClient[F]:
      override def get(
          key: String,
          doublyEncryptedFrontPartBase64: String,
      ): EitherT[F, String, String] =
        EitherT
          .apply:
            sttpClient(
              GatewayApi.GatewayRequest(key, doublyEncryptedFrontPartBase64),
            ).map(sanitize)
          .map(_.singlyEncryptedBase64)
