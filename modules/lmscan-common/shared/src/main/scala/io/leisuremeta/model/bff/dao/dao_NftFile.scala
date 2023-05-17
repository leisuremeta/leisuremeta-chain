package io.leisuremeta.chain.lmscan.common.model.dao

final case class NftFile(
    tokenId: String,
    tokenDefId: String,
    collectionName: String,
    nftName: String,
    nftUri: String,
    creatorDescription: String,
    dataUrl: String,
    rarity: String,
    creator: String,
    eventTime: Long,
    createdAt: Long,
    owner: String,
)
