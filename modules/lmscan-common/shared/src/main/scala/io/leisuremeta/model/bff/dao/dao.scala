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

  case Block(
      number: Long,
      hash: String,
      parentHash: String,
      txCount: Long,
      eventTime: Long,
      createdAt: Long,
  )

  case Nft(
      tokenId: String,
      txHash: String,
      action: String,
      fromAddr: String,
      toAddr: String,
      eventTime: Long,
      createdAt: Long,
  )

  case NftFile(
      tokenId: String,
      tokenDefId: String,
      collectionName: String,
      nftName: String,
      nftUri: String,
      creatorDescription: String,
      dataUrl: String,
      rarity: String,
      creator: String,
      eventTime: Long,
      createdAt: Long,
      owner: String,
  )

  case Summary(
      id: Long,
      lmPrice: Double,
      blockNumber: Long,
      totalAccounts: Long,
      createdAt: Long,
      totalTxSize: Long,
      total_balance: String,
  )
