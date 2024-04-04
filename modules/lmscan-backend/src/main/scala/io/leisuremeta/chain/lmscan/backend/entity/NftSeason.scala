package io.leisuremeta.chain.lmscan.backend.entity

import io.leisuremeta.chain.lmscan.common.model.NftSeasonModel

final case class NftSeason(
    nftName: String,
    tokenId: String,
    tokenDefId: String,
    creator: String,
    rarity: String,
    dataUrl: String,
):
  def toModel: NftSeasonModel =
    NftSeasonModel(
      Some(nftName),
      Some(tokenId),
      Some(tokenDefId),
      Some(creator),
      Some(rarity),
      Some(dataUrl),
    )
