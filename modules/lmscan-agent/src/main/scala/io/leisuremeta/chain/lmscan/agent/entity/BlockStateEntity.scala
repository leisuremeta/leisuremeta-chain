package io.leisuremeta.chain.lmscan.agent.entity


final case class BlockStateEntity(
  hash: String,
  number: Long,
  eventTime: Long,
  json: String,
  isBuild: Boolean, // default: false
)
