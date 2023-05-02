package io.leisuremeta.chain.gateway.eth.common
package client

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.bifunctor.*
import cats.syntax.functor.*

import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.model.Uri
import sttp.tapir.DecodeResult
import sttp.tapir.client.sttp.SttpClientInterpreter

trait GatewayApiClient[F[_]]:
  def apply(
      key: String,
      doublyEncryptedFrontPartBase64: String,
  ): EitherT[F, String, String]

object GatewayApiClient:
  def make[F[_]: Async](uri: Uri): Resource[F, GatewayApiClient[F]] =
    ArmeriaCatsBackend
      .resource[F]()
      .map: backend =>
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
          override def apply(
              key: String,
              doublyEncryptedFrontPartBase64: String,
          ): EitherT[F, String, String] =
            EitherT
              .apply:
                sttpClient(
                  GatewayApi.GatewayRequest(key, doublyEncryptedFrontPartBase64),
                ).map(sanitize)
              .map(_.singlyEncryptedBase64)
