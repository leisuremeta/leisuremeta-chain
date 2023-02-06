package io.leisuremeta.chain.lmscan.backend.entity

final case class Summary(
  lmPrice: Double,
  blockNumber: Long,
  txCountsIn24Hour: Long,
  totalAccounts: Long,
)
