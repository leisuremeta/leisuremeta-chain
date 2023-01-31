package io.leisuremeta.chain.lmscan.backend.model

final case class BlockInfo(
    number: Long,
    hash: String,
    txCount: Long,
    createdAt: Long,
)
