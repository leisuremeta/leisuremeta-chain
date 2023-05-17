package io.leisuremeta.chain.lmscan.common.model.dao

final case class Summary(
    id: Long,
    lmPrice: Double,
    blockNumber: Long,
    totalTxSize: Long,
    totalAccounts: Long,
    createdAt: Long,
)
