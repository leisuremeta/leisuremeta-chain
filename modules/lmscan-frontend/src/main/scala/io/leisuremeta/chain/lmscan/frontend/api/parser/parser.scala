package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*

object Parser:
  case class Tx(
      hash: String,
      txType: String,
      fromAddr: List[String],
      toAddr: List[String],
      amount: Long,
      blockHash: String,
      eventTime: Int,
      createdAt: Int,
  )
  case class Payload(tx_list: List[Tx])
  case class TxList(totalCount: Int, totalPages: Int, payload: Payload)

  implicit val txDecoder: Decoder[Tx] = deriveDecoder
  implicit val txEncoder: Encoder[Tx] = deriveEncoder
