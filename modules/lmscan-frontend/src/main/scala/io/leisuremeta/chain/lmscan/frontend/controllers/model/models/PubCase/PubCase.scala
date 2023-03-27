package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

enum PubCase:
//   baseurl
//   &pageNo=0
//   &sizePerRequest=10
//   &accountAddr=playnomm or hash

  case BlockPub(
      page: Int = 1,
      pub_m1: String = "",
      pub_m2: PageResponse[BlockInfo] =
        new PageResponse[BlockInfo](0, 0, List()),
  ) extends PubCase

  case TxPub(
      page: Int = 1,
      sizePerRequest: Int = 10,
      accountAddr: String = "",
      blockHash: String = "",
      pub_m1: String = "",
      pub_m2: PageResponse[TxInfo] = new PageResponse[TxInfo](0, 0, List()),
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

  case AccountDetailPub(
      hash: String = "",
      pub_m1: String = "",
      pub_m2: AccountDetail = new AccountDetail,
  ) extends PubCase
