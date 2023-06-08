package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.leisuremeta.chain.lmscan.common.model.dao.*

object Dao2Dto:
  def tx(dao: List[Tx]) = dao.map(d =>
    DTO_Tx(
      hash = Some(d.hash),
      txType = Some(d.txType),
      createdAt = Some(d.createdAt),
      tokenType = Some(d.tokenType),
      outputVals = d.outputVals,
      blockNumber = Some(d.blockNumber),
      inputHashs = d.inputHashs,
      amount = d.amount,
      subType = Some(d.subType),
    ),
  )
  def account(dao: List[Account]) = dao.map(d => d)(0)
