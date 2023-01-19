package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.entity.Tx

final case class AccountDetail(
    address: String,
    balance: Double,
    value: Double,
    txHistory: Seq[Tx],
)
