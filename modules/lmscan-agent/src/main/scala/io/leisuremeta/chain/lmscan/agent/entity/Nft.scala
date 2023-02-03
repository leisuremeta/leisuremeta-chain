package io.leisuremeta.chain.lmscan.agent.entity

final case class Nft(
    tokenId: String,
    txHash: String,
    rarity: Option[String],
    owner: String,
    action: String,
    fromAddr: Option[String],
    toAddr: String,
    eventTime: Long,
    createdAt: Long,
)