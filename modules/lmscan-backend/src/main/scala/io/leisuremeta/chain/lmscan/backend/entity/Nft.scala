package io.leisuremeta.chain.lmscan.backend.entity

final case class Nft(
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    eventTime: Long,
    createdAt: Long,
    tokenId: String,
)

// final case class NftActivity(
//     txHash: String,
//     action: String,
//     fromAddr: String,
//     toAddr: String,
//     createdAt: Long,
// )
