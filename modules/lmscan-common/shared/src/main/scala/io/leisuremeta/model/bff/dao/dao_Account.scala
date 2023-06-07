package io.leisuremeta.chain.lmscan.common.model.dao

final case class Account(
    address: String,
    createdAt: Long,
    eventTime: Long,
    balance: Option[BigDecimal] = None,
    amount: Option[BigDecimal] = None,
)
