package io.leisuremeta.chain.lmscan.common.model.dto

final case class DTO_Tx(
    hash: Option[String] = None,
    txType: Option[String] = None, // col_name : type
    fromAddr: Option[String] = None,
    toAddr: Seq[String] = Seq(""),
    blockHash: Option[String] = None,
    eventTime: Option[Long] = None,
    createdAt: Option[Long] = None,
    tokenType: Option[String] = None,
    outputVals: Option[Seq[String]] = None,
    json: Option[String] = None,
    blockNumber: Option[Long] = None,
    inputHashs: Option[Seq[String]] = None,
    subType: Option[String] = None,
)
