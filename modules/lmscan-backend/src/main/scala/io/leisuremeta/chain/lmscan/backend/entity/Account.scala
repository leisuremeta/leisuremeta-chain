package io.leisuremeta.chain.lmscan.backend.entity

final case class Account(
    address: String,
    balance: Long,
    amount: Long,
    createdAt: Long,
)
