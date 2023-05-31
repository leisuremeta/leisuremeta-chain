package io.leisuremeta.chain.gateway.eth.common

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class GatewayConf(
    ethChainId: Int,
    ethContractAddress: String,
    gatewayEthAddress: String,
    gatewayEndpoint: String,
    localServerPort: Int,
    lmEndpoint: String,
    kmsAlias: String,
    encryptedEthEndpoint: String,
    encryptedDatabaseEndpoint: String,
    databaseTableName: String,
    databaseValueColumn: String,
    targetGateway: String,
) derives ConfigReader

object GatewayConf:
  def loadOrThrow(): GatewayConf =
    ConfigSource.default.loadOrThrow[GatewayConf]
