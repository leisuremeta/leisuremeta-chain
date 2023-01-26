package io.leisuremeta.chain.lmscan.agent.entity

import java.lang.reflect.Field
import io.getquill.Quoted

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Seq[String],
    amount: Double, // value
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
)
