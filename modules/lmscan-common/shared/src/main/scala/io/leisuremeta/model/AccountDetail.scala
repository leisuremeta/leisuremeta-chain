package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class AccountDetail(
  address: Option[String] = None,
  balance: Option[BigDecimal] = None,
  value: Option[BigDecimal] = None,
  totalCount: Long = 0L,
  totalPages: Int = 0,
  payload: Seq[TxInfo] = Seq(),
) extends ApiModel

object AccountDetail:
  given Decoder[AccountDetail] = deriveDecoder[AccountDetail]
