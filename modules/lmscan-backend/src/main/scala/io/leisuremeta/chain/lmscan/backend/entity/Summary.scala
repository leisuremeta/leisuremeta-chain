package io.leisuremeta.chain.lmscan.backend.entity

final case class Summary(
    id: Long,
    lmPrice: Double,
    blockNumber: Long,
    totalAccounts: Long,
    createdAt: Long,
    totalTxSize: BigDecimal,
    totalBalance: BigDecimal,
)
