package io.leisuremeta.chain.lmscan
package frontend

import common.model._

sealed trait Msg

case object NoneMsg extends Msg
case object ErrorMsg extends Msg

case class GlobalInput(s: String) extends Msg
case object GlobalSearch extends Msg
case object Init extends Msg

case class UpdateDetailPage(param: ApiModel) extends Msg

sealed trait ListMsg extends Msg:
  val value: Int

case object UpdateSummary extends Msg
case object UpdateChart extends Msg
case object UpdateChartAll extends Msg
case class UpdateBlockPage(value: Int) extends ListMsg
case class UpdateTxPage(value: Int) extends ListMsg
case class UpdateNftPage(value: Int) extends ListMsg
case class UpdateNftTokenPage(id: String, value: Int) extends ListMsg
case class UpdateAccPage(value: Int) extends ListMsg
case class UpdateSearch(v: Int) extends Msg

case class UpdateModel(model: ApiModel) extends Msg

case class PopupMsg(value: Boolean) extends Msg

enum RouterMsg extends Msg:
  case NoOp
  case NavigateTo(page: Page)
  case ToDetail(page: Model)
  case NavigateToUrl(url: String)
