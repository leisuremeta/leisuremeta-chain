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
    nftPage: NftModel = NftModel(),
    nftTokenPage: NftTokenModel = NftTokenModel(),
    txDetail: TxDetail = TxDetail(),
    blcDetail: BlockDetail = BlockDetail(),
    accDetail: AccountDetail = AccountDetail(),
    nftDetail: NftDetail = NftDetail(),
    chartData: SummaryChart = SummaryChart(),
)

case class SummaryChart(list: List[SummaryModel] = List()) extends ApiModel

trait ListPage[T] extends ApiModel:
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

final case class NftModel(
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: Option[NftList] = None,
) extends ListPage[NftInfoModel]
final case class NftTokenModel(
    id: String = "",
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: Option[NftTokenList] = None,
) extends ListPage[NftSeasonModel]

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

final case class NftList(
    totalCount: Long = 0,
    totalPages: Long = 0,
    payload: List[NftInfoModel] = List(),
) extends ListType[NftInfoModel]

final case class NftTokenList(
    totalCount: Long = 0,
    totalPages: Long = 0,
    payload: List[NftSeasonModel] = List(),
) extends ListType[NftSeasonModel]
