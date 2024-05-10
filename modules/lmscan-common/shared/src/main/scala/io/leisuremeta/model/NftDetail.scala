package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class NftDetail(
    nftFile: Option[NftFileModel] = None,
    activities: Option[Seq[NftActivity]] = None,
) extends ApiModel 

object NftDetail:
  given Decoder[NftDetail] = deriveDecoder[NftDetail]
