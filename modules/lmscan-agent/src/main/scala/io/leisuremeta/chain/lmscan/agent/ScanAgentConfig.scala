package io.leisuremeta.chain.lmscan.agent

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class ScanAgentConfig(
    base: String,
    remote: DBConfig,
    local: DBConfig,
    scan: String,
    market: MarketConfig,
    es: ESConfig,
) derives ConfigReader

final case class MarketConfig(
    key: String,
    token: Int,
)
final case class ESConfig(
    key: String,
    lm: String,
    addrs: List[String],
)

final case class DBConfig(
    driver: String,
    url: String,
    user: Option[String],
    password: Option[String],
)

object ScanAgentConfig:
  def load: ScanAgentConfig = ConfigSource.default.loadOrThrow[ScanAgentConfig]
