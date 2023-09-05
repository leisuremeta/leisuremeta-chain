package io.leisuremeta.chain.lmscan.common.model

final case class BlockDetail(
    hash: Option[String] = None,
    parentHash: Option[String] = None,
    number: Option[Long] = None,
    timestamp: Option[Long] = None,
    txCount: Option[Long] = None,
    txs: Option[Seq[TxInfo]] = None,
) extends ApiModel
