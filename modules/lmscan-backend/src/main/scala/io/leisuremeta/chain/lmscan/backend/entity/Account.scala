package io.leisuremeta.chain.lmscan.backend.entity

final case class Account(
    address: String,
    createdAt: Long,
    eventTime: Long,
)
