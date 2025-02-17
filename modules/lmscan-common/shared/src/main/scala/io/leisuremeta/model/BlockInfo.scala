package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class BlockInfo(
    number: Option[Long] = None,
    hash: Option[String] = None,
    txCount: Option[Long] = None,
    createdAt: Option[Long] = None,
    proposer: Option[String] = None,
) extends ApiModel

object BlockInfo:
  given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
