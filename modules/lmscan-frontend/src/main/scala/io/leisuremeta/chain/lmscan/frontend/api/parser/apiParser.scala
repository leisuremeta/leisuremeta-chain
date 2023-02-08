package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.parser.*

case class ApiData(
    id: Int,
    lmPrice: Double,
    blockNumber: Long,
    txCountInLatest24h: Long,
    totalAccounts: Long,
    createdAt: Long,
)

object ApiParser:
  given apiDecoder: Decoder[ApiData]   = deriveDecoder[ApiData]
  def decodeParser(body: String) = decode[ApiData](body)
