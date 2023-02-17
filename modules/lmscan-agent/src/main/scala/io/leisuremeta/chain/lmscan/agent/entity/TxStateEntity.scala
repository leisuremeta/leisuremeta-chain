package io.leisuremeta.chain.lmscan.agent.entity

final case class TxStateEntity(
  hash: String,
  eventTime: Long,
  json: String,
  blockHash: String,
)
