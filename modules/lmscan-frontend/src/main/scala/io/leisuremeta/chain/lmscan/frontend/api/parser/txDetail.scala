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
  given transferDecoder: Decoder[Transfer] = deriveDecoder[Transfer]
  given txDetailDecoder: Decoder[TxDetail] = deriveDecoder[TxDetail]
  def decodeParser(body: String)           = decode[TxDetail](body)

  given transferEncoder: Encoder[Transfer] = deriveEncoder[Transfer]
  given txDetailEncoder: Encoder[TxDetail] = deriveEncoder[TxDetail]
