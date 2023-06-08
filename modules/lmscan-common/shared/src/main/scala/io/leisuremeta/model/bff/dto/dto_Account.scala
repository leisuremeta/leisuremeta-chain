package io.leisuremeta.chain.lmscan.common.model.dto

final case class DTO_Account(
    address: Option[String] = None,
    createdAt: Option[Long] = None,
    eventTime: Option[Long] = None,
    balance: Option[BigDecimal] = None,
    amount: Option[BigDecimal] = None,
)
