package io.leisuremeta.chain.lmscan.agent.entity

final case class SummaryEntity(
  id: Long,
  lmPrice: Double,
  blockNumber: Long,
  txCountInLatest24h: Long, //tx_count_in_latest24h
  totalAccounts: Long,
  createdAt: Long,
)
