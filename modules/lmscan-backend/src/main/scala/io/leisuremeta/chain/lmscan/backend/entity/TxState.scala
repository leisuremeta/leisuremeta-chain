package io.leisuremeta.chain.lmscan.backend.entity

final case class TxState(
    hash: String,
    blockHash: String,
    json: String,
    eventTime: Long,
    createdAt: Long,
)
