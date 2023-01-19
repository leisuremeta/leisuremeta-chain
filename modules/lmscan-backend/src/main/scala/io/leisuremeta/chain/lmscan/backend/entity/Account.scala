package io.leisuremeta.chain.lmscan.backend.entity

final case class Account(
    address: String,
    balance: Double,
    amount: Double,
    createdAt: Long,
)
