package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class TxDetail(
    hash: Option[String] = None,
    createdAt: Option[Long] = None,
    json: Option[String] = None,
) extends ApiModel 

object TxDetail:
  given Decoder[TxDetail] = deriveDecoder[TxDetail]
