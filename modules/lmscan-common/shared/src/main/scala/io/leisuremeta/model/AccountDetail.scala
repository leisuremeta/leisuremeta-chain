package io.leisuremeta.chain.lmscan.common.model

final case class AccountDetail(
  address: Option[String] = None,
  balance: Option[BigDecimal] = None,
  value: Option[BigDecimal] = None,
  totalCount: Long = 0L,
  totalPages: Int = 0,
  payload: Seq[TxInfo] = Seq(),
) extends ApiModel
