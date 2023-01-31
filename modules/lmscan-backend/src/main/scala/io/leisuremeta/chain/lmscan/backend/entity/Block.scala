package io.leisuremeta.chain.lmscan.backend.entity

final case class Block(
    number: Long,
    hash: String,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
)
