package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import io.leisuremeta.chain.lmscan.common.model.TransferHist

object TxDetailParser:
  given transferDecoder: Decoder[TransferHist] = deriveDecoder[TransferHist]
  given txDetailDecoder: Decoder[TxDetail]     = deriveDecoder[TxDetail]
  def decodeParser(body: String)               = decode[TxDetail](body)

  given transferEncoder: Encoder[TransferHist] = deriveEncoder[TransferHist]
  given txDetailEncoder: Encoder[TxDetail]     = deriveEncoder[TxDetail]
