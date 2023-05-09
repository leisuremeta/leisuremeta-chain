package io.leisuremeta.chain.gateway.eth.common

import cats.data.EitherT
import cats.effect.{Async, Resource}

import scodec.bits.ByteVector
import sttp.client3.*

import client.*

object GatewayResource:

  def getAllResource[F[_]: Async](conf: GatewayConf) = for
    kms <- GatewayKmsClient
      .make[F](conf.kmsAlias)
      .mapK(EitherT.liftK[F, String])
    web3jBytes <- Resource.eval:
      EitherT.fromEither:
        ByteVector.fromHexDescriptive(conf.encryptedEthEndpoint)
    web3jPlaintextResource <- Resource.eval(kms.decrypt(web3jBytes.toArray))
    web3jPlaintext         <- web3jPlaintextResource.mapK(EitherT.liftK[F, String])
    web3jEndpoint = String(web3jPlaintext, "UTF-8")
    web3j <- GatewayWeb3Service.web3Resource[F](web3jEndpoint)
      .mapK(EitherT.liftK[F, String])
    dbBytes <- Resource.eval:
      EitherT.fromEither:
        ByteVector.fromHexDescriptive(conf.encryptedDatabaseEndpoint)
    dbPlaintextResource <- Resource.eval(kms.decrypt(dbBytes.toArray))
    dbPlaintext         <- dbPlaintextResource.mapK(EitherT.liftK[F, String])
    dbEndpoint = String(dbPlaintext, "UTF-8")
    db <- GatewayDatabaseClient
      .make[F](dbEndpoint, conf.databaseTableName, conf.databaseValueColumn)
      .mapK(EitherT.liftK[F, String])
    api <- GatewayApiClient
      .make[F](uri"${conf.lmEndpoint}")
      .mapK(EitherT.liftK[F, String])
  yield (kms, web3j, db, api)
