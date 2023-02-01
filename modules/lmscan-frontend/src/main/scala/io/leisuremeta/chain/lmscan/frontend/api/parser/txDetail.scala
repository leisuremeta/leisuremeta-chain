package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class TxDetail(
    hash: String,
    createdAt: Int,
    signer: String,
    txType: String,
    tokenType: String,
    inputHashs: List[String],
    transferHist: List[Transfer],
    json: String,
)

case class Transfer(
    toAddress: String,
    value: Double,
)

object TxDetailParser:
  implicit val transferDecoder: Decoder[Transfer] = deriveDecoder[Transfer]
  implicit val txDetailDecoder: Decoder[TxDetail] = deriveDecoder[TxDetail]
  def decodeParser(body: String)                  = decode[TxDetail](body)

  implicit val transferEncoder: Encoder[Transfer] = deriveEncoder[Transfer]
  implicit val txDetailEncoder: Encoder[TxDetail] = deriveEncoder[TxDetail]
