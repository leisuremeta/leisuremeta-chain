package io.leisuremeta.chain.lmscan.backend2.entity

final case class Summary(
    id: Long,
    lmPrice: Double,
    blockNumber: Long,
    totalTxSize: Long,
    totalAccounts: Long,
    createdAt: Long,
)
