package io.leisuremeta.chain.lmscan.backend2.entity

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
