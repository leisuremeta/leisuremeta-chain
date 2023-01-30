package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*


case class TxList2(totalCount: Int, totalPages: Int, payload: List[Tx2])
case class Tx2(
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
  implicit val txlistDecoder: Decoder[TxList2] = deriveDecoder[TxList2]
  implicit val txDecoder: Decoder[Tx2] = deriveDecoder[Tx2]
  def decodeParser(body: String) = decode[TxList2](body)
