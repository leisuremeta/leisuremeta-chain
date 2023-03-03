package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel

object ApiParser:
  given apiDecoder: Decoder[SummaryModel] = deriveDecoder[SummaryModel]
  def decodeParser(body: String)          = decode[SummaryModel](body)
