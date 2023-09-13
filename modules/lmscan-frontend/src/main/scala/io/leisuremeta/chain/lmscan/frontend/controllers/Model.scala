package io.leisuremeta.chain.lmscan
package frontend

import common.model._

final case class Model(
    popup: Boolean = false,
    searchValue: String = "",
    page: Page = MainPage,
    summary: SummaryBoard = SummaryBoard(),
    blcPage: BlockModel = BlockModel(),
    txPage: TxModel = TxModel(),
    nftPage: NftModel = NftModel(),
    nftTokenPage: NftTokenModel = NftTokenModel(),
    accPage: AccModel = AccModel(),
    txDetail: TxDetail = TxDetail(),
    blcDetail: BlockDetail = BlockDetail(),
    accDetail: AccountDetail = AccountDetail(),
    nftDetail: NftDetail = NftDetail(),
    chartData: SummaryChart = SummaryChart(),
)

trait ListPage[T] extends ApiModel:
    val page: Int
    val size: Int
    val searchPage: Int
    val list: Option[ListType[T]]

trait ListType[T] extends ApiModel:
    val totalCount: Long
    val totalPages: Int
    val payload: Seq[T]

final case class TxModel(
    page: Int = 1,
    size: Int = 20,
    searchPage: Int = 1,
    list: Option[TxList] = None,
) extends ListPage[TxInfo]

final case class BlockModel(
    page: Int = 1,
    size: Int = 20,
    searchPage: Int = 1,
    list: Option[BlcList] = None,
) extends ListPage[BlockInfo]

final case class NftModel(
    page: Int = 1,
    size: Int = 20,
    searchPage: Int = 1,
    list: Option[NftList] = None,
) extends ListPage[NftInfoModel]
final case class NftTokenModel(
    id: String = "",
    page: Int = 1,
    size: Int = 20,
    searchPage: Int = 1,
    list: Option[NftTokenList] = None,
) extends ListPage[NftSeasonModel]
final case class AccModel(
    page: Int = 1,
    size: Int = 20,
    searchPage: Int = 1,
    list: Option[AccList] = None,
) extends ListPage[AccountInfo]

final case class BlcList(
    totalCount: Long = 0,
    totalPages: Int = 0,
    payload: Seq[BlockInfo] = Seq(),
) extends ListType[BlockInfo]

final case class TxList(
    totalCount: Long = 0,
    totalPages: Int = 0,
    payload: Seq[TxInfo] = Seq(),
) extends ListType[TxInfo]

final case class NftList(
    totalCount: Long = 0,
    totalPages: Int = 0,
    payload: Seq[NftInfoModel] = Seq(),
) extends ListType[NftInfoModel]

final case class NftTokenList(
    totalCount: Long = 0,
    totalPages: Int = 0,
    payload: Seq[NftSeasonModel] = Seq(),
) extends ListType[NftSeasonModel]

final case class AccList(
    totalCount: Long = 0,
    totalPages: Int = 0,
    payload: Seq[AccountInfo] = Seq(),
) extends ListType[AccountInfo]
