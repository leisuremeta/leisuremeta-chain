package io.leisuremeta.chain.lmscan
package frontend

import common.model._

sealed trait Msg

case object NoneMsg extends Msg
case object ErrorMsg extends Msg

case class GlobalInput(s: String) extends Msg
case object GlobalSearch extends Msg

sealed trait DetailMsg extends Msg:
  val param: String

case class UpdateTxDetailPage(param: String) extends DetailMsg
case class UpdateBlcDetailPage(param: String) extends  DetailMsg
case class UpdateAccDetailPage(param: String) extends  DetailMsg
case class UpdateNftDetailPage(param: String) extends  DetailMsg

sealed trait ListMsg extends Msg:
  val value: Int

case object UpdateSummary extends Msg
case object UpdateChart extends Msg
case class UpdateBlockPage(value: Int) extends ListMsg
case class UpdateTxPage(value: Int) extends ListMsg
case class UpdateNftPage(value: Int) extends ListMsg
case class UpdateNftTokenPage(id: String, value: Int) extends ListMsg

sealed trait SearchMsg extends Msg:
  val v: Int
case class UpdateBlcsSearch(v: Int) extends SearchMsg
case class UpdateTxsSearch(v: Int) extends SearchMsg

enum PageMsg extends Msg:
  // 데이터 업데이트
  case UpdateBlc(value: BlcList) extends PageMsg
  case UpdateTx(value: TxList) extends PageMsg
  case UpdateNft(value: NftList) extends PageMsg
  case UpdateNftToken(value: NftTokenList) extends PageMsg
  case Update1(value: SummaryModel) extends PageMsg
  case UpdateChart(value: List[SummaryModel]) extends PageMsg

  case UpdateTxDetail(v: TxDetail) extends PageMsg

  case UpdateBlcDetail(v: BlockDetail) extends  PageMsg

  case UpdateAccDetail(v: AccountDetail) extends  PageMsg
  case UpdateNftDetail(v: NftDetail) extends  PageMsg

case class PopupMsg(value: Boolean) extends Msg

enum RouterMsg extends Msg:
  case NoOp
  case NavigateTo(page: Page)
  case NavigateToUrl(url: String)
