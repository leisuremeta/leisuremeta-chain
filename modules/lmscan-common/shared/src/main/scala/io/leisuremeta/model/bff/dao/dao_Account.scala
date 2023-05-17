package io.leisuremeta.chain.lmscan.common.model.dao

final case class Account(
    address: String,
    balance: BigDecimal,
    amount: BigDecimal,
    createdAt: Long,
)
