package io.leisuremeta.chain.lmscan.backend.entity

final case class Tx(
    hash: String,
    signer: String,
    txType: String, // col_name : type
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
    tokenType: String,
    blockNumber: Long,
    subType: String,
)
