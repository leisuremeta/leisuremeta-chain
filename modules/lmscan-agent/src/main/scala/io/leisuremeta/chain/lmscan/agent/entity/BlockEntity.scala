package io.leisuremeta.chain.lmscan.agent.entity

final case class BlockEntity(
    hash: String,
    number: Long,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
) 