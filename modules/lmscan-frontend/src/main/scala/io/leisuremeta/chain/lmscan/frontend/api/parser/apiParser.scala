package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*

case class ApiData(
    id: Option[Int] = None,
    lmPrice: Option[Double] = None,
    blockNumber: Option[Long] = None,
    txCountInLatest24h: Option[Long] = None,
    totalAccounts: Option[Long] = None,
    createdAt: Option[Long] = None,
)

object ApiParser:
  given apiDecoder: Decoder[ApiData]   = deriveDecoder[ApiData]
  def decodeParser(body: String) = decode[ApiData](body)
