package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class TxList(totalCount: Int, totalPages: Int, payload: List[Tx])
case class Tx(
    hash: String,
    blockNumber: Int,
    createdAt: Int,
    txType: String,
    tokenType: String,
    signer: String,
    value: String,
)

object TxParser:
  given txlistDecoder: Decoder[TxList] = deriveDecoder[TxList]
  given txDecoder: Decoder[Tx]         = deriveDecoder[Tx]
  def decodeParser(body: String)       = decode[TxList](body)
