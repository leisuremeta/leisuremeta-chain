package io.leisuremeta.chain.lmscan.common.model

final case class TxDetail(
    hash: Option[String] = None,
    createdAt: Option[Long] = None,
    json: Option[String] = None,
) extends ApiModel 

