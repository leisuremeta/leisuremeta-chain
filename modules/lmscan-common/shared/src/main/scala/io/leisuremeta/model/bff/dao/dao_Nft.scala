package io.leisuremeta.chain.lmscan.common.model.dao

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
