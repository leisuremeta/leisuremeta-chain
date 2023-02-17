package io.leisuremeta.chain.lmscan.backend.entity

final case class Nft(
    tokenId: String,
    txHash: String,
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
