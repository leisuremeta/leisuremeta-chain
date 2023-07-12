package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.NftDetail
import io.leisuremeta.chain.lmscan.common.model.NftFileModel
import io.leisuremeta.chain.lmscan.common.model.NftActivity

object NftDetailParser:
  given nftFileDecoder: Decoder[NftFileModel]      = deriveDecoder[NftFileModel]
  given nftActivitiesDecoder: Decoder[NftActivity] = deriveDecoder[NftActivity]
  given txDetailDecoder: Decoder[NftDetail]        = deriveDecoder[NftDetail]
  def decodeParser(body: String)                   = decode[NftDetail](body)
