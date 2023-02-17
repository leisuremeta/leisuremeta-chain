package io.leisuremeta.chain.lmscan.backend.entity

final case class Account(
    address: String,
    balance: BigDecimal,
    amount: BigDecimal,
    createdAt: Long,
)
