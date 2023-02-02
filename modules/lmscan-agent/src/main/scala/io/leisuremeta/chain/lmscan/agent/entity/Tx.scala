package io.leisuremeta.chain.lmscan.agent.entity

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    tokenType: String,
    fromAddr: String,
    toAddr: Seq[String],
    blockHash: String,
    blockNumber: Long,
    eventTime: Long,
    createdAt: Long,
    inputHashs: Seq[String],
    outputVals: Seq[String],
    json: String,
) 
