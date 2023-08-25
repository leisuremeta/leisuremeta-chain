package io.leisuremeta.chain.gateway.eth.common

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class GatewaySimpleConf(
    ethChainId: Int,
    ethLmContractAddress: String,
    ethMultisigContractAddress: String,
    gatewayEthAddress: String,
    depositExempts: Seq[String],
    lmEndpoint: String,
    kmsAlias: String,
    encryptedEthEndpoint: String,
    encryptedEthPrivate: String,
    encryptedLmPrivate: String,
    targetGateway: String,
) derives ConfigReader

object GatewaySimpleConf:
  def loadOrThrow(): GatewaySimpleConf =
    ConfigSource.default.loadOrThrow[GatewaySimpleConf]
