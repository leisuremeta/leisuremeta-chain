package io.leisuremeta.chain.lmscan.common.model.dao

case class Account(
    address: Option[String] = None,
    createdAt: Option[Long] = None,
    eventTime: Option[Long] = None,
    balance: Option[BigDecimal] = None,
    amount: Option[BigDecimal] = None,
) extends DAO
