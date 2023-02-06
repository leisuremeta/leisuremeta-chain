package io.leisuremeta.chain.lmscan.agent.entity

final case class BlockSavedLog(
    hash: String,
    number: Long,
    json: String,
    eventTime: Long,
    createdAt: Long,
)
