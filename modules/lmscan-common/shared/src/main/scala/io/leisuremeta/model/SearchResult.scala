package io.leisuremeta.chain.lmscan.common.model

enum SearchResult:
  case acc(item: AccountDetail)
  case tx(item: TxDetail)
  case blc(item: BlockDetail)
  case nft(item: NftDetail)
  case empty
