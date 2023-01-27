package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}

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

object Parser:
  implicit val txDecoder: Decoder[Tx] = deriveDecoder
  implicit val txEncoder: Encoder[Tx] = deriveEncoder

object CustomEncoder:
  implicit val txListEncoder: Encoder[TxList] = txList =>
    Json.obj(
      "totalCount" -> txList.totalCount.asJson,
      "totalPages" -> txList.totalPages.asJson,
      //   "payload"    -> txList.payload.asJson,
    )

// object CustomDecoder:
//   implicit val txListDecoder: Decoder[TxList] =
//     Decoder.forProduct3("totalCount", "totalPages", "payload")(TxList.apply)
