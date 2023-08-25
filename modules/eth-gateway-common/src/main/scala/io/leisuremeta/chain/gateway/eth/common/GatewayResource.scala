package io.leisuremeta.chain.gateway.eth.common

import cats.data.EitherT
import cats.effect.{Async, Resource}

import org.web3j.protocol.Web3j
import scodec.bits.ByteVector
import sttp.client3.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend

import client.*
import cats.effect.std.Dispatcher

object GatewayResource:
  def getAllResource[F[_]: Async](conf: GatewayConf): Resource[
    EitherT[F, String, *],
    (GatewayKmsClient[F], Web3j, GatewayDatabaseClient[F], SttpBackend[F, Any]),
  ] = for
    kms <- GatewayKmsClient
      .make[F](conf.kmsAlias)
      .mapK(EitherT.liftK[F, String])
    web3jBytes <- Resource.eval:
      EitherT.fromEither:
        ByteVector.fromBase64Descriptive(conf.encryptedEthEndpoint)
    web3jPlaintextResource <- Resource.eval(kms.decrypt(web3jBytes.toArray))
    web3jPlaintext <- web3jPlaintextResource.mapK(EitherT.liftK[F, String])
    web3jEndpoint = String(web3jPlaintext, "UTF-8")
    web3j <- GatewayWeb3Service
      .web3Resource[F](web3jEndpoint)
      .mapK(EitherT.liftK[F, String])
    dbBytes <- Resource.eval:
      EitherT.fromEither:
        ByteVector.fromBase64Descriptive(conf.encryptedDatabaseEndpoint)
    dbPlaintextResource <- Resource.eval(kms.decrypt(dbBytes.toArray))
    dbPlaintext         <- dbPlaintextResource.mapK(EitherT.liftK[F, String])
    dbEndpoint = String(dbPlaintext, "UTF-8")
    db <- GatewayDatabaseClient
      .make[F](dbEndpoint, conf.databaseTableName, conf.databaseValueColumn)
      .mapK(EitherT.liftK[F, String])
    sttp <- ArmeriaCatsBackend.resource[F]().mapK(EitherT.liftK[F, String])
    dispatcher <- Dispatcher.parallel[F].mapK(EitherT.liftK[F, String])
    server <- GatewayServer
      .make[F](dispatcher, conf.localServerPort, db, kms)
      .mapK(EitherT.liftK[F, String])
  yield (kms, web3j, db, sttp)

  def getSimpleResource[F[_]: Async](
      conf: GatewaySimpleConf,
  ): Resource[EitherT[F, String, *], (GatewayKmsClient[F], Web3j, SttpBackend[F, Any])] = for
    kms <- GatewayKmsClient
      .make[F](conf.kmsAlias)
      .mapK(EitherT.liftK[F, String])
    web3jBytes <- Resource.eval:
      EitherT.fromEither:
        ByteVector.fromBase64Descriptive(conf.encryptedEthEndpoint)
    web3jPlaintextResource <- Resource.eval(kms.decrypt(web3jBytes.toArray))
    web3jPlaintext <- web3jPlaintextResource.mapK(EitherT.liftK[F, String])
    web3jEndpoint = String(web3jPlaintext, "UTF-8")
    web3j <- GatewayWeb3Service
      .web3Resource[F](web3jEndpoint)
      .mapK(EitherT.liftK[F, String])
    sttp <- ArmeriaCatsBackend.resource[F]().mapK(EitherT.liftK[F, String])
  yield (kms, web3j, sttp)
