package io.leisuremeta.chain.lmscan.backend.model

final case class NftActivity(
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: String,
    createdAt: Long,
)
