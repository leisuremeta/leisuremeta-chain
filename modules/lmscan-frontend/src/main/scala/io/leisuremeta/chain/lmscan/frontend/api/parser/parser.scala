package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class TxList(totalCount: Int, totalPages: Int, payload: List[Tx])
case class Tx(
    hash: String,
    txType: String,
    fromAddr: String,
    toAddr: List[String],
    amount: Double,
    blockHash: String,
    eventTime: Int,
    createdAt: Int,
)

object TxParser:
  implicit val txlistDecoder: Decoder[TxList] = deriveDecoder[TxList]
  implicit val txDecoder: Decoder[Tx]         = deriveDecoder[Tx]
  def decodeParser(body: String)              = decode[TxList](body)
