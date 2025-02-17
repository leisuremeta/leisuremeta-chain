package io.leisuremeta.chain.lmscan.backend.entity

import io.leisuremeta.chain.lmscan.common.model.BlockInfo

final case class Block(
    number: Long,
    hash: String,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
    proposer: String
):
    def toModel = BlockInfo(
        Some(number),
        Some(hash),
        Some(txCount),
        Some(createdAt),
        Some(proposer),
    )
