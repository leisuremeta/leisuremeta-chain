package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.Tx
// import io.leisuremeta.chain.lmscan.backend.entity.NftActivity
import io.leisuremeta.chain.lmscan.backend.entity.Nft

final case class NftDetail(
    file: String,
    collectionName: String,
    nftName: String,
    tokenId: String,
    rarity: String,
    owner: String,
    activityList: Seq[Nft],
):
  def this() =
    this(null, null, null, null, null, null, null)
  def this(payload: Seq[Nft]) =
    this(null, null, null, null, null, null, payload)
