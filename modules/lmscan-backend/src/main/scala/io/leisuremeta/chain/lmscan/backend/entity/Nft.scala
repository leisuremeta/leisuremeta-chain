package io.leisuremeta.chain.lmscan
package backend
package entity

final case class Nft(
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    eventTime: Long,
    createdAt: Long,
    tokenId: String,
)
