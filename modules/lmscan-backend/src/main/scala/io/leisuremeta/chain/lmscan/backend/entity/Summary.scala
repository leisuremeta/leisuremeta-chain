package io.leisuremeta.chain.lmscan.backend.entity

final case class Summary(
  lmPrice: Double,
  blockNumber: Long,
  txCountInLatest24h: Long,
  totalAccounts: Long,
  createdAt: Long,
)
