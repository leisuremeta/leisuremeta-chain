package io.leisuremeta.chain.lmscan.backend.model

final case class TxInfo(
    hash: String,
    blockNumber: Long,
    createdAt: Long,
    txType: String,
    tokenType: String,
    signer: String,
    value: Option[String],
)
