package io.leisuremeta.chain.lmscan.backend2.entity

// import io.getquill.Quoted

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Option[String] = None,
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
    tokenType: String,
    outputVals: Option[String],
    json: String,
    blockNumber: Long,
    inputHashs: Option[String],
    amount: Option[Double],
    subType: String,
    displayYn: Boolean,
)
