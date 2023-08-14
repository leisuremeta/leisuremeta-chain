package io.leisuremeta.chain.gateway.eth.setup

import pureconfig.*
import pureconfig.generic.derivation.default.*

import EthGatewaySetupConfig.*

final case class EthGatewaySetupSimpleConfig(
    ethPrivate: String,
    lmPrivate: String,
    kmsAlias: String,
    ethEndpoint: String,
) derives ConfigReader

object EthGatewaySetupSimpleConfig:

  def apply(): EthGatewaySetupSimpleConfig =
    ConfigSource.default.loadOrThrow[EthGatewaySetupSimpleConfig]

  final case class DbConfig(
      host: String,
      port: Int,
      db: String,
      table: String,
      valueColumn: String,
      user: String,
      password: String,
  )

  final case class DbWriteAccountConfig(
      user: String,
      password: String,
  )
