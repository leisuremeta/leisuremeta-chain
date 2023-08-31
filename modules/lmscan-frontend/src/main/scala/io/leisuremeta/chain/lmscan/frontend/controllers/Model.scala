package io.leisuremeta.chain.lmscan
package frontend

import common.model._

final case class Model(
    popup: Boolean = false,
    searchValue: String = "",
    page: Page = MainPage,
    summary: SummaryModel = SummaryModel(),
    blcPage: BlockModel = BlockModel(),
    txPage: TxModel = TxModel(),
    txDetail: TxDetail = TxDetail(),
    blcDetail: BlockDetail = BlockDetail(),
    accDetail: AccountDetail = AccountDetail(),
    nftDetail: NftDetail = NftDetail(),
    chartData: List[SummaryModel] = List(),
)

trait ListPage[T]:
    val page: Int
    val size: Int
    val searchPage: Int
    val list: Option[ListType[T]]

trait ListType[T]:
    val totalCount: Long
    val totalPages: Long
    val payload: List[T]

final case class TxModel(
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: Option[TxList] = None,
) extends ListPage[TxInfo]

final case class BlockModel(
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: Option[BlcList] = None,
) extends ListPage[BlockInfo]

final case class BlcList(
    totalCount: Long = 0,
    totalPages: Long = 0,
    payload: List[BlockInfo] = List(),
) extends ListType[BlockInfo]

final case class TxList(
    totalCount: Long = 0,
    totalPages: Long = 0,
    payload: List[TxInfo] = List(),
) extends ListType[TxInfo]
