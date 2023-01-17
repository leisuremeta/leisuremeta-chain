package io.leisuremeta.chain.lmscan.backend.entity

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Seq[String],
    amount: Double, // value
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
)
