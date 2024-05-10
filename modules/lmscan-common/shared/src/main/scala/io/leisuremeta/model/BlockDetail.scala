package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class BlockDetail(
  hash: Option[String] = None,
  parentHash: Option[String] = None,
  number: Option[Long] = None,
  timestamp: Option[Long] = None,
  txCount: Option[Long] = None,
  totalCount: Long = 0L,
  totalPages: Int = 0,
  payload: Seq[TxInfo] = Seq(),
) extends ApiModel

object BlockDetail:
  given Decoder[BlockDetail] = deriveDecoder[BlockDetail]
