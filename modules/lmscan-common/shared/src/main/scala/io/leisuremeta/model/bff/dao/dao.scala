package io.leisuremeta.chain.lmscan.common.model

enum DAO:
  case Tx(
      hash: String,
      txType: String, // col_name : type
      fromAddr: String,
      toAddr: Option[String] = None,
      blockHash: String,
      eventTime: Long,
      createdAt: Long,
      tokenType: String,
      outputVals: Option[String],
      json: String,
      blockNumber: Long,
      inputHashs: Option[String],
      amount: Option[Double],
      subType: String,
      displayYn: Boolean,
  )

  case Account(
      address: Option[String] = None,
      createdAt: Option[Long] = None,
      eventTime: Option[Long] = None,
      balance: Option[BigDecimal] = None,
      amount: Option[BigDecimal] = None,
  )
