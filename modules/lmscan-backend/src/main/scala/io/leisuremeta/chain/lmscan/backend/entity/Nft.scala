package io.leisuremeta.chain.lmscan.backend.entity

import io.leisuremeta.chain.lmscan.backend.model.NftActivity

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

// final case class NftActivity(
//     txHash: String,
//     action: String,
//     fromAddr: String,
//     toAddr: String,
//     createdAt: Long,
// )
