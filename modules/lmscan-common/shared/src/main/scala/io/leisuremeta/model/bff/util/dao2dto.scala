package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.leisuremeta.chain.lmscan.common.model.dao.*

// final case class Tx(
//     hash: String,
//     txType: String, // col_name : type
//     fromAddr: String,
//     toAddr: Option[String] = None,
//     blockHash: String,
//     eventTime: Long,
//     createdAt: Long,
//     tokenType: String,
//     outputVals: Option[String],
//     json: String,
//     blockNumber: Long,
//     inputHashs: Option[String],
//     amount: Option[Double],
//     subType: String,
//     displayYn: Boolean,
// )

object Dao2Dto:
  def tx(dao: List[Tx]) = dao.map(d =>
    DTO_Tx(
      Some(d.hash),
      Some(d.txType),
      Some(d.fromAddr),
      d.toAddr,
      Some(d.blockHash),
      Some(d.eventTime),
      Some(d.createdAt),
      Some(d.tokenType),
      d.outputVals,
      Some(d.json),
      Some(d.blockNumber),
      d.inputHashs,
      d.amount,
      Some(d.subType),
      d.displayYn,
    ),
  )
  def account(dao: List[Account]) = dao.map(d => d)
  // def account(dao: List[Account]) = dao.map(d =>
  //   DTO_Account(
  //     Some(d.address),
  //     Some(d.createdAt),
  //     Some(d.eventTime),
  //     Some(d.balance),
  //     Some(d.amount),
  //   ),
  // )
