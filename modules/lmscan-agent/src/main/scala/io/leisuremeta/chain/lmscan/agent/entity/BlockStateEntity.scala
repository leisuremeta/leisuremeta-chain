package io.leisuremeta.chain.lmscan.agent.entity


final case class BlockStateEntity(
  id: Long,
  eventTime: Long,
  json: String,
  isBuild: Boolean, // default: false
)
