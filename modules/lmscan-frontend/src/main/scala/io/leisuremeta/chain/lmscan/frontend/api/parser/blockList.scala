package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockParser:
  given blocklistDecoder: Decoder[PageResponse[BlockInfo]] =
    deriveDecoder[PageResponse[BlockInfo]]
  given blockDecoder: Decoder[BlockInfo] = deriveDecoder[BlockInfo]
  def decodeParser(body: String)         = decode[PageResponse[BlockInfo]](body)
