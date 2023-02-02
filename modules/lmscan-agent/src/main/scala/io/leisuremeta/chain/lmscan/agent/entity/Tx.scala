package io.leisuremeta.chain.lmscan.agent.entity
import io.leisuremeta.chain.lmscan.agent.model.id

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
) extends id:
    def id: String =
        this.hash
