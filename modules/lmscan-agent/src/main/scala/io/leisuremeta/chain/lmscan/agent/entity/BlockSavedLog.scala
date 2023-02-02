package io.leisuremeta.chain.lmscan.agent.entity

final case class BlockSavedLog(
    hash: String,
    number: Long,
    eventTime: Long,
    createdAt: Long,
)
