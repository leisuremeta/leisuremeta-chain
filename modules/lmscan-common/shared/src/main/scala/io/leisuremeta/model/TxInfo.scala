package io.leisuremeta.chain.lmscan.common.model

final case class TxInfo(
    hash: Option[String] = None,
    blockNumber: Option[Long] = None,
    createdAt: Option[Long] = None,
    txType: Option[String] = None,
    tokenType: Option[String] = None,
    signer: Option[String] = None,
    subType: Option[String] = None,
    inOut: Option[String] = None,
    value: Option[String] = None,
) extends ApiModel 
