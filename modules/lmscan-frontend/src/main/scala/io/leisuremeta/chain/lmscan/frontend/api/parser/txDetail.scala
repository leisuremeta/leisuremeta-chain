package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.common.model.TransferHist

// case class TxDetail(
//     hash: Option[String] = None,
//     createdAt: Option[Int] = None,
//     signer: Option[String] = None,
//     txType: Option[String] = None,
//     tokenType: Option[String] = None,
//     inputHashs: Option[List[String]] = None,
//     transferHist: Option[List[Transfer]] = None,
//     json: Option[String] = None,
// )

// case class Transfer(
//     toAddress: Option[String] = None,
//     value: Option[Double] = None,
// )

object TxDetailParser:
    given transferDecoder: Decoder[TransferHist] = deriveDecoder[TransferHist]
    given txDetailDecoder: Decoder[TxDetail] = deriveDecoder[TxDetail]
    def decodeParser(body: String)           = decode[TxDetail](body)

    given transferEncoder: Encoder[TransferHist] = deriveEncoder[TransferHist]
    given txDetailEncoder: Encoder[TxDetail] = deriveEncoder[TxDetail]
