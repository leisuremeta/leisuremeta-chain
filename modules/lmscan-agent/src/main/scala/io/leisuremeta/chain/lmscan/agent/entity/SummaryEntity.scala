package io.leisuremeta.chain.lmscan.agent.entity

final case class SummaryEntity(
  id: Long,
  lmPrice: Double,
  blockNumber: Long,
  txCountsIn24Hour: Long,
  totalAccounts: Long,
)
