package io.leisuremeta.chain.lmscan.common.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class NftFileModel(
    tokenId: Option[String] = None,
    tokenDefId: Option[String] = None,
    collectionName: Option[String] = None,
    nftName: Option[String] = None,
    nftUri: Option[String] = None,
    creatorDescription: Option[String] = None,
    dataUrl: Option[String] = None,
    rarity: Option[String] = None,
    creator: Option[String] = None,
    eventTime: Option[Long] = None,
    createdAt: Option[Long] = None,
    owner: Option[String] = None,
) extends ApiModel 

object NftFileModel:
  given Decoder[NftFileModel] = deriveDecoder[NftFileModel]
