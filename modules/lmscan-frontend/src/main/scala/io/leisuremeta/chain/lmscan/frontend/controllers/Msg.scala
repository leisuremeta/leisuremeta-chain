package io.leisuremeta.chain.lmscan
package frontend

import common.model._
import scalajs.js

sealed trait Msg
sealed trait GlobalMsg extends Msg

case object ErrorMsg extends Msg

case class GlobalInput(s: String) extends GlobalMsg
case class UpdateTime(t: js.Date) extends GlobalMsg
case object GlobalSearch extends Msg
case object ListSearch extends Msg
case object Init extends Msg

case class UpdateDetailPage(param: ApiModel) extends Msg

case object DrawChart extends Msg
case class UpdateSearch(v: Int) extends Msg

case class UpdateModel(model: ApiModel) extends Msg
case class UpdateListModel[ApiModel](model: PageResponse[ApiModel]) extends Msg
case class UpdateBlcs(model: PageResponse[BlockInfo]) extends Msg
case class UpdateTxs(model: PageResponse[TxInfo]) extends Msg
case class UpdateSample[ApiModel](model: PageResponse[ApiModel]) extends Msg

trait RouterMsg extends Msg
case class ToPage(model: Model) extends RouterMsg
case class NavigateToUrl(url: String) extends RouterMsg
case object EmptyRoute extends RouterMsg
