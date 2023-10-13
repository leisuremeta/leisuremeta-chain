package io.leisuremeta.chain.lmscan.common.model

import java.time.Instant

final case class AccountInfo(
  address: Option[String],
  balance: Option[BigDecimal],
  updated: Option[Instant],
  value: Option[BigDecimal]
) extends ApiModel
