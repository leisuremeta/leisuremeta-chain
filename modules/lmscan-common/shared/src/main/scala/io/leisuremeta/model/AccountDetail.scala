package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.TxInfo

final case class AccountDetail(
  address: Option[String] = None,
  balance: Option[BigDecimal] = None,
  value: Option[BigDecimal] = None,
  txHistory: Option[Seq[TxInfo]] = None,    
)
