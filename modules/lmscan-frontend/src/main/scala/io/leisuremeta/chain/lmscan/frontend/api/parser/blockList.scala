package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

// case class BlockList(
//   totalCount: Option[Int] = None,
//   totalPages: Option[Int] = None,
//   payload: Option[List[Block]] = None,
// )

// case class Block(
//     number: Option[Int] = None,
//     hash: Option[String] = None,
//     createdAt: Option[Int] = None,
//     txCount: Option[Int] = None,
// )

// object BlockParser:
//   given blocklistDecoder: Decoder[BlockList] = deriveDecoder[BlockList]
//   given blockDecoder: Decoder[Block]         = deriveDecoder[Block]
//   def decodeParser(body: String)             = decode[BlockList](body)

object BlockParser:
  given blocklistDecoder: Decoder[PageResponse[BlockInfo]] = deriveDecoder[PageResponse[BlockInfo]]
  given blockDecoder: Decoder[BlockInfo]         = deriveDecoder[BlockInfo]
  def decodeParser(body: String)             = decode[PageResponse[BlockInfo]](body)
