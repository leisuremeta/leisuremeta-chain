package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

enum PubCase:
  case BlockPub(
      page: Int = 1,
      sizePerRequest: Int = 10,
      pub_m1: String = "",
      pub_m2: PageResponse[BlockInfo] =
        new PageResponse[BlockInfo](1, 1, Range(1, 11).map(d => new BlockInfo)),
  ) extends PubCase

  case TxPub(
      page: Int = 1,
      sizePerRequest: Int = 10,
      accountAddr: String = "",
      blockHash: String = "",
      pub_m1: String = "",
      pub_m2: PageResponse[TxInfo] =
        new PageResponse[TxInfo](1, 1, Range(1, 11).map(d => new TxInfo)),
  ) extends PubCase

  case BoardPub(
      page: Int = 1,
      pub_m1: String = "",
      pub_m2: SummaryModel = new SummaryModel,
  ) extends PubCase

  case BlockDetailPub(
      hash: String = "",
      pub_m1: String = "",
      pub_m2: BlockDetail = new BlockDetail,
  ) extends PubCase

  case TxDetailPub(
      hash: String = "",
      pub_m1: String = "",
      pub_m2: TxDetail = new TxDetail,
  ) extends PubCase

  case NftDetailPub(
      hash: String = "",
      pub_m1: String = "",
      pub_m2: NftDetail = new NftDetail,
  ) extends PubCase

  case AccountDetailPub(
      hash: String = "",
      pub_m1: String = "",
      pub_m2: AccountDetail = new AccountDetail,
  ) extends PubCase
