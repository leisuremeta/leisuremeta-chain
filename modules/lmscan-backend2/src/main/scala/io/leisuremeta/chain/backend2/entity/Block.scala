package io.leisuremeta.chain.lmscan.backend2.entity

final case class Block(
    number: Long,
    hash: String,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
)
