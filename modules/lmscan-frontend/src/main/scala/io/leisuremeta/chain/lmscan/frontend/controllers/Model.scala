package io.leisuremeta.chain.lmscan
package frontend

import io.circe._
import io.circe.generic.semiauto._
import common.model._

final case class Model(
    pointer: Int = 1,
    searchValue: String = "",
    toggle: Boolean = false,
    temp: String = "",
    detail_button: Boolean = false,
    subtype: String = "",
    pageLimit: Int = 50,
    popup: Boolean = false,
    lmprice: Double = 0.0,

    page: Page = MainPage,
    // for mainpage ,,
    mainPage: MainModel = MainModel(SummaryModel(), BlcList(), TxList()),
    blcPage: BlockModel = BlockModel(1, 10, 1, BlcList()),
    txPage: TxModel = TxModel(1, 10, 1, TxList()),
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
    page: Int,
    size: Int,
    searchPage: Int,
    list: TxList,
) extends ListPage[TxInfo]

object TxModel:
    given Decoder[TxModel] = deriveDecoder[TxModel]

final case class BlockModel(
    page: Int,
    size: Int,
    searchPage: Int,
    list: BlcList,
) extends ListPage[BlockInfo]

object BlockModel:
    given Decoder[BlockModel] = deriveDecoder[BlockModel]

final case class MainModel(
    summary: SummaryModel,
    bList: BlcList,
    tList: TxList,
)

final case class BlcList(
    totalCount: Option[Long],
    totalPages: Option[Long],
    payload: List[BlockInfo],
) extends ListType[BlockInfo]

object BlcList:
    given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
    given Decoder[BlcList] = deriveDecoder[BlcList]
    def apply(): BlcList = BlcList(None, None, List())

final case class TxList(
    totalCount: Option[Long],
    totalPages: Option[Long],
    payload: List[TxInfo],
) extends ListType[TxInfo]

object TxList:
    given Decoder[TxInfo] = deriveDecoder[TxInfo]
    given Decoder[TxList] = deriveDecoder[TxList]
    def apply(): TxList = TxList(None, None, List())

object MainModel:
    given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
    given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
    given Decoder[TxInfo] = deriveDecoder[TxInfo]
    given Decoder[BlcList] = deriveDecoder[BlcList]
    given Decoder[TxList] = deriveDecoder[TxList]
