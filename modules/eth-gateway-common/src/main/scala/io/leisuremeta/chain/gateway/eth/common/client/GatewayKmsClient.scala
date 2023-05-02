package io.leisuremeta.chain.gateway.eth.common.client

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.functor.*

import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.model.DecryptRequest

trait GatewayKmsClient[F[_]]:
  def decrypt(
      cipherText: Array[Byte],
  ): EitherT[F, String, Resource[F, Array[Byte]]]

object GatewayKmsClient:
  def make[F[_]: Async](alias: String): Resource[F, GatewayKmsClient[F]] =
    Resource
      .fromAutoCloseable(Async[F].delay(KmsAsyncClient.create()))
      .map: kmsAsyncClient =>
        new GatewayKmsClient[F]:
          override def decrypt(
              cipherText: Array[Byte],
          ): EitherT[F, String, Resource[F, Array[Byte]]] = Async[F]
            .attemptT:
              Async[F]
                .fromCompletableFuture:
                  Async[F].delay:
                    kmsAsyncClient.decrypt:
                      DecryptRequest
                        .builder()
                        .keyId(s"alias/${alias}")
                        .ciphertextBlob(SdkBytes.fromByteArray(cipherText))
                        .build()
                .map: decryptResponse =>
                  Resource
                    .make:
                      Async[F].delay(
                        decryptResponse.plaintext().asByteArrayUnsafe(),
                      )
                    .apply: byteArray =>
                      Async[F].delay:
                        val emptyArray =
                          Array.fill[Byte](byteArray.length)(0x00)
                        System.arraycopy(
                          emptyArray,
                          0,
                          byteArray,
                          0,
                          byteArray.length,
                        )
            .leftMap(_.getMessage())
