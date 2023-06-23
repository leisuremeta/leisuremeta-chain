package io.leisuremeta.chain.lmscan.common.model.dto

final case class DTO_Tx_type1(
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
final case class DTO_Tx_type2(
    hash: Option[String] = None,
    txType: Option[String] = None,
    createdAt: Option[Long] = None,
    tokenType: Option[String] = None,
    outputVals: Option[String] = None,
    blockNumber: Option[Long] = None,
    inputHashs: Option[String] = None,
    amount: Option[Double] = None,
    subType: Option[String] = None,
)
final case class DTO_Tx_count(
    count: Long = 0,
)
