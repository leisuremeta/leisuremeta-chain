package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.DTO.Tx.Tx_self
import io.leisuremeta.chain.lmscan.common.model.DTO.Tx.Tx_Type2

// import io.leisuremeta.chain.lmscan.common.model.dto.*
// import io.leisuremeta.chain.lmscan.common.model.dao.*

type TxLike = Tx_self | Tx_Type2

object Dao2Dto:

  def tx2tx_self(dao: DAO.Tx) =
    DTO.Tx.Tx_self(
      dao.hash,
      dao.txType,
      dao.fromAddr,
      dao.toAddr,
      dao.blockHash,
      dao.eventTime,
      dao.createdAt,
      dao.tokenType,
      dao.outputVals,
      dao.json,
      dao.blockNumber,
      dao.inputHashs,
      dao.amount,
      dao.subType,
      dao.displayYn,
    )

  def tx2tx_type2(dao: DAO.Tx) =
    DTO.Tx.Tx_Type2(
      hash = Some(dao.hash),
      // txType = Some(dao.txType),
      // createdAt = Some(dao.createdAt),
      // tokenType = Some(dao.tokenType),
      // outputVals = dao.outputVals,
      // blockNumber = Some(dao.blockNumber),
      // inputHashs = dao.inputHashs,
      // amount = dao.amount,
      // subType = Some(dao.subType),
    )

  def genericTxDto(dto: Option[String])(dao: DAO.Tx): TxLike =
    dto match
      case None =>
        tx2tx_self(dao)
      case Some(v) =>
        v match
          case "tx_type2" =>
            tx2tx_type2(dao)

  def account(dao: List[DAO.Account]) = dao.map(d => d)(0)
