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
    value: Double,
)

object TxParser:
  implicit val blocklistDecoder: Decoder[TxList] = deriveDecoder[TxList]
  implicit val blockDecoder: Decoder[Tx]         = deriveDecoder[Tx]
  def decodeParser(body: String)                 = decode[TxList](body)

case class BlockList(totalCount: Int, totalPages: Int, payload: List[Block])
case class Block(
    number: Int,
    hash: String,
    createdAt: Int,
    txCount: Int,
)

object BlockParser:
  implicit val txlistDecoder: Decoder[BlockList] = deriveDecoder[BlockList]
  implicit val txDecoder: Decoder[Block]         = deriveDecoder[Block]
  def decodeParser(body: String)                 = decode[BlockList](body)
