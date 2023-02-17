package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class TxDetail(
    hash: Option[String] = None,
    createdAt: Option[Int] = None,
    signer: Option[String] = None,
    txType: Option[String] = None,
    tokenType: Option[String] = None,
    inputHashs: Option[List[String]] = None,
    transferHist: Option[List[Transfer]] = None,
    json: Option[String] = None,
)

case class Transfer(
    toAddress: Option[String] = None,
    // value: Option[Double] = None,
    value: Option[String] = None,
)

object TxDetailParser:
    given transferDecoder: Decoder[Transfer] = deriveDecoder[Transfer]
    given txDetailDecoder: Decoder[TxDetail] = deriveDecoder[TxDetail]
    def decodeParser(body: String)           = decode[TxDetail](body)

    given transferEncoder: Encoder[Transfer] = deriveEncoder[Transfer]
    given txDetailEncoder: Encoder[TxDetail] = deriveEncoder[TxDetail]
