package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.circe.generic.auto.*

// case class ApiData(
//     id: Option[Int] = None,
//     lmPrice: Option[Double] = None,
//     blockNumber: Option[Long] = None,
//     txCountInLatest24h: Option[Long] = None,
//     totalAccounts: Option[Long] = None,
//     createdAt: Option[Long] = None,
// )

object ApiParser:
  // given apiDecoder: Decoder[SummaryModel] = deriveDecoder[SummaryModel]
  // def decodeParser(body: String)          = decode[SummaryModel](body)

  def decodeParser(data: String) = decode[SummaryModel](data)
  def getParsedData(data: String) =
    decodeParser(data).getOrElse("디코딩 실패\n")
