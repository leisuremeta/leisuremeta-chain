package io.leisuremeta.chain.lmscan.backend.entity

import io.leisuremeta.chain.lmscan.agent.model.id

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
) extends id:
    def id: String =
        this.tokenId
