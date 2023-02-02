package io.leisuremeta.chain.lmscan.agent.model

final case class PHeader(
  number: Long,
  parentHash: String,
  timestamp: String,
)
