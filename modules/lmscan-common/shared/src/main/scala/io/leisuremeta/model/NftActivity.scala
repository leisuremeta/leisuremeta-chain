package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class NftActivity(
    txHash: Option[String] = None,
    action: Option[String] = None,
    fromAddr: Option[String] = None,
    toAddr: Option[String] = None,
    createdAt: Option[Long] = None,
) extends ApiModel 

object NftActivity:
  given Decoder[NftActivity] = deriveDecoder[NftActivity]
