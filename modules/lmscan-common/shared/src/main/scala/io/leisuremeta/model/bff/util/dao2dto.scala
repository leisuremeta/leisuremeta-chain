package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.dto.*
import io.leisuremeta.chain.lmscan.common.model.dao.*

object Dao2Dto:
  def tx_type1(dao: List[Tx]) = dao.map(d =>
    DTO.Tx.type1(
      d.hash,
      d.txType,
      d.fromAddr,
      d.toAddr,
      d.blockHash,
      d.eventTime,
      d.createdAt,
      d.tokenType,
      d.outputVals,
      d.json,
      d.blockNumber,
      d.inputHashs,
      d.amount,
      d.subType,
      d.displayYn,
    ),
  )
  def tx_type2(dao: List[Tx]) = dao.map(d =>
    DTO.Tx.type2(
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
