package io.leisuremeta.chain.lmscan
package frontend

import io.circe._
import io.circe.generic.semiauto._
import common.model._

final case class Model(
    appStates: List[StateCase],
    pointer: Int = 1,
    searchValue: String = "",
    toggle: Boolean = false,
    temp: String = "",
    commandMode: CommandCaseMode = CommandCaseMode.Production,
    commandLink: CommandCaseLink,
    detail_button: Boolean = false,
    tx_total_page: String = "1",
    tx_current_page: String = "1",
    subtype: String = "",
    block_total_page: String = "1",
    block_current_page: String = "1",
    pageLimit: Int = 50,
    popup: Boolean = false,
    lmprice: Double = 0.0,

    // for mainpage ,,
    mainPage: MainModel = MainModel(SummaryModel(), BlcList(), TxList()),
)

final case class MainModel(
    summary: SummaryModel,
    blcList: BlcList,
    txList: TxList,
)

final case class BlcList(
    totalCount: Option[Long],
    totalPages: Option[Long],
    payload: List[BlockInfo],
)

object BlcList:
    given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
    given Decoder[BlcList] = deriveDecoder[BlcList]
    def apply(): BlcList = BlcList(None, None, List())

final case class TxList(
    totalCount: Option[Long],
    totalPages: Option[Long],
    payload: List[TxInfo],
)

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
