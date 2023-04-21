package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.circe.generic.auto.*

object ApiParser:

  def decodeParser(data: String) = decode[SummaryModel](data)
  def getParsedData(data: String) =
    decodeParser(data).getOrElse("디코딩 실패\n")
