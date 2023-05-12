package io.leisuremeta.chain.gateway.eth.common

import cats.data.EitherT
import cats.effect.{Async, Resource}

import scodec.bits.ByteVector
import client.*

object GatewayDecryptService:
  def getPlainTextResource[F[_]
    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient](
      key: String,
  ): EitherT[F, String, Resource[F, Array[Byte]]] =
    for
      doublyEncryptedFrontOption <- GatewayDatabaseClient[F].select(key)
      doublyEncryptedFront <- EitherT.fromOption(
        doublyEncryptedFrontOption,
        s"Value not found for key: ${key}",
      )
      singlyEncryptedBase64 <- GatewayApiClient[F].get(
        key,
        doublyEncryptedFront,
      )
      bytes <- EitherT.fromEither:
        ByteVector.fromBase64Descriptive(singlyEncryptedBase64)
      plaintextResource <- GatewayKmsClient[F].decrypt(bytes.toArray)
    yield plaintextResource

  def getEth[F[_]
    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient]
      : EitherT[F, String, Resource[F, Array[Byte]]] =
    getPlainTextResource[F]("ETH")

  def getLm[F[_]
    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient]
      : EitherT[F, String, Resource[F, Array[Byte]]] =
    getPlainTextResource[F]("LM")

  def getLmD[F[_]
    : Async: GatewayApiClient: GatewayDatabaseClient: GatewayKmsClient]
      : EitherT[F, String, Resource[F, Array[Byte]]] =
    getPlainTextResource[F]("LM-D")
