package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.TxInfo

object TxParser:
  given txlistDecoder: Decoder[PageResponse[TxInfo]] =
    deriveDecoder[PageResponse[TxInfo]]
  given txDecoder: Decoder[TxInfo] = deriveDecoder[TxInfo]
  def decodeParser(body: String)   = decode[PageResponse[TxInfo]](body)
