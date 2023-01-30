package io.leisuremeta.chain.lmscan.backend.model

final case class BlockDetail(
    hash: String,
    parentHash: String,
    blockNumber: Long,
    timestamp: Long,
    txCount: Long,
    txs: Seq[TxInfo],
)
