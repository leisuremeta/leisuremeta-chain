package io.leisuremeta.chain.lmscan.backend.entity

final case class Nft(
    tokenId: String,
    txHash: String,
    rarity: String,
    owner: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    eventTime: Long,
    createdAt: Long,
)