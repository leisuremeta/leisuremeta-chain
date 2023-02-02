package io.leisuremeta.chain.lmscan.agent.entity

import io.leisuremeta.chain.lmscan.agent.model.id

final case class Account(
    address: String,
    balance: Double,
    amount: Double,
    createdAt: Long,
) extends id:
    def id: String =
        this.address
