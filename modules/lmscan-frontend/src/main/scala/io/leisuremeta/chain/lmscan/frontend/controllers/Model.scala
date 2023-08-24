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
)

trait ListPage[T]:
    val page: Int
    val size: Int
    val searchPage: Int
    val list: ListType[T]

trait ListType[T]:
    val totalCount: Option[Long]
    val totalPages: Option[Long]
    val payload: List[T]

final case class TxModel(
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: TxList = TxList(),
) extends ListPage[TxInfo]

final case class BlockModel(
    page: Int = 1,
    size: Int = 10,
    searchPage: Int = 1,
    list: BlcList = BlcList(),
) extends ListPage[BlockInfo]

final case class BlcList(
    totalCount: Option[Long] = None,
    totalPages: Option[Long] = None,
    payload: List[BlockInfo] = List(),
) extends ListType[BlockInfo]

final case class TxList(
    totalCount: Option[Long] = None,
    totalPages: Option[Long] = None,
    payload: List[TxInfo] = List(),
) extends ListType[TxInfo]
