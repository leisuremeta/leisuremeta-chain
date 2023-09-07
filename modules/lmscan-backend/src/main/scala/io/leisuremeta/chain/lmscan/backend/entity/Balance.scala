package io.leisuremeta.chain.lmscan.backend.entity

final case class Balance(
    address: String,
    free: BigDecimal = BigDecimal(0),
    locked: BigDecimal = BigDecimal(0),
    updatedAt: Long,
    createdAt: Long,
)
