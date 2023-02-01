package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class BlockDetail(
    hash: String,
    parentHash: String,
    number: Int,
    timestamp: Int,
    txCount: Int,
    txs: List[Tx],
)

object BlockDetailParser:
  implicit val txDecoder: Decoder[Tx] = deriveDecoder[Tx]
  implicit val blockDetailDecoder: Decoder[BlockDetail] =
    deriveDecoder[BlockDetail]
  def decodeParser(body: String) = decode[BlockDetail](body)
