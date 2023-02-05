package io.leisuremeta.chain.lmscan.agent.entity

final case class TxStateEntity(
  id: Long,
  eventTime: Long,
  json: String,
  blockHash: String,
  isBuild: Boolean, // default: false
)
