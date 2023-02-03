package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class BlockList(totalCount: Int, totalPages: Int, payload: List[Block])

case class Block(
    number: Int,
    hash: String,
    createdAt: Int,
    txCount: Int,
)

object BlockParser:
  given blocklistDecoder: Decoder[BlockList] = deriveDecoder[BlockList]
  given blockDecoder: Decoder[Block]         = deriveDecoder[Block]
  def decodeParser(body: String)             = decode[BlockList](body)
