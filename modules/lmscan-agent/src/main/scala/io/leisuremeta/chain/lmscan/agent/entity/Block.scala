package io.leisuremeta.chain.lmscan.agent.entity

import io.leisuremeta.chain.lmscan.agent.model.id

final case class Block(
    number: Long,
    hash: String,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
) extends id:
    def id: String =
        this.hash
