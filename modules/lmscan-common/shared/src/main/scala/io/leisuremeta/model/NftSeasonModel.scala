package io.leisuremeta.chain.lmscan.common.model

final case class NftSeasonModel(
    nftName: Option[String] = None,
    tokenId: Option[String] = None,
    tokenDefId: Option[String] = None,
    creator: Option[String] = None,
    rarity: Option[String] = None,
    thumbUrl: Option[String] = None,
    collection: Option[String] = None,
) extends ApiModel
