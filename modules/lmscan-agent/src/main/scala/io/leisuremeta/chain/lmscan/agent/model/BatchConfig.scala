package io.leisuremeta.chain.lmscan.agent.model

import com.typesafe.config.Config
import cats.effect.IO
import com.typesafe.config.ConfigFactory


case class BatchConfig (
  baseUri: String,

)

object BatchConfig:
  
  val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

  def fromConfig(config: Config): BatchConfig =
    BatchConfig(
      config.getString("baseUri")
    )