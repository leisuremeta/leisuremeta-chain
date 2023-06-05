package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.leisuremeta.chain.lmscan.common.model.dao.*

// object Utills:
//   def dao2dto(dao: Seq[Tx]): Seq[DTO_Tx] = dao.map(d =>
//     DTO_Tx(
//       Some(d.hash),
//       Some(d.txType),
//       Some(d.fromAddr),
//       d.toAddr,
//       Some(d.blockHash),
//       Some(d.eventTime),
//       Some(d.createdAt),
//       Some(d.tokenType),
//       d.outputVals,
//       Some(d.json),
//       Some(d.blockNumber),
//       d.inputHashs,
//       Some(d.subType),
//     ),
//   )
object Utills:
  def dao2dto(dao: Seq[Tx]) = dao
