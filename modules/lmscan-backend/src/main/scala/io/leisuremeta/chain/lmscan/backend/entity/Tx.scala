package io.leisuremeta.chain.lmscan.backend.entity

import io.getquill.Quoted

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    tokenType: String,
    fromAddr: String,
    toAddr: Option[Seq[String]],
    blockHash: String,
    blockNumber: Long,
    eventTime: Long,
    createdAt: Long,
    inputHashs: Option[Seq[String]],
    outputVals: Option[Seq[String]],
    json: String,
    // amount: Double,
)
