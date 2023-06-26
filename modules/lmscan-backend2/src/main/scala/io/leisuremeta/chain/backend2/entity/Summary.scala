package io.leisuremeta.chain.lmscan.backend2.entity

final case class Summary(
    id: Long,
    lmPrice: Double,
    blockNumber: Long,
    totalAccounts: Long,
    createdAt: Long,
    totalTxSize: Long,
    total_balance: Long,
)
