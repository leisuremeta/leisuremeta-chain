package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

case class BlockList(
  totalCount: Option[Int] = None,
  totalPages: Option[Int] = None,
  payload: Option[List[Block]] = None,
)

case class Block(
    number: Option[Int] = None,
    hash: Option[String] = None,
    createdAt: Option[Int] = None,
    txCount: Option[Int] = None,
)

object BlockParser:
  given blocklistDecoder: Decoder[BlockList] = deriveDecoder[BlockList]
  given blockDecoder: Decoder[Block]         = deriveDecoder[Block]
  def decodeParser(body: String)             = decode[BlockList](body)
