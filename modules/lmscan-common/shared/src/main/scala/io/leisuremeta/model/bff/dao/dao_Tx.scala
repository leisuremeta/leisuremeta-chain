package io.leisuremeta.chain.lmscan.common.model.dao

import io.getquill.Quoted

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Seq[String],
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
    tokenType: String,
    outputVals: Option[Seq[String]],
    json: String,
    blockNumber: Long,
    inputHashs: Option[Seq[String]],
    subType: String,
)