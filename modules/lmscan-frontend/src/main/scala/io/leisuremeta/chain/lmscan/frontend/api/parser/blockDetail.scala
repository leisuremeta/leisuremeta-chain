package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.BlockDetail
import io.leisuremeta.chain.lmscan.common.model.TxInfo

object BlockDetailParser:
  given txDecoder: Decoder[TxInfo]               = deriveDecoder[TxInfo]
  given blockDetailDecoder: Decoder[BlockDetail] = deriveDecoder[BlockDetail]
  def decodeParser(body: String)                 = decode[BlockDetail](body)
