package io.leisuremeta.chain.gateway.eth.setup

import pureconfig.*
import pureconfig.generic.derivation.default.*

import EthGatewaySetupConfig.*

final case class EthGatewaySetupConfig(
    ethPrivate: String,
    lmPrivate: String,
    depositDb: DbConfig,
    withdrawDb: DbConfig,
    dbWriteAccount: DbWriteAccountConfig,
    depositKmsAlias: String,
    withdrawKmsAlias: String,
    ethEndpoint: String,
) derives ConfigReader

object EthGatewaySetupConfig:

  def apply(): EthGatewaySetupConfig =
    ConfigSource.default.loadOrThrow[EthGatewaySetupConfig]

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
