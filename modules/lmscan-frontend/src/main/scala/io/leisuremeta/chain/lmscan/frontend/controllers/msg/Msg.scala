package io.leisuremeta.chain.lmscan
package frontend

import common.model._
import io.circe.Json

sealed trait Msg

enum PageMsg extends Msg:
  case GotoObserver(page: Int)             extends PageMsg
  case BackObserver                        extends PageMsg
  case RolloBack                           extends PageMsg
  case None                                extends PageMsg

  case UpdateBlcs extends PageMsg
  case UpdateBlcsSearch(v: Int) extends PageMsg
  case UpdateTxsSearch(v: Int) extends PageMsg
  case UpdateTxs extends PageMsg

  // 데이터 업데이트
  case UpdateBlockPage(value: Int) extends PageMsg
  case UpdateTxPage(value: Int) extends PageMsg
  case UpdateBlc(value: BlcList) extends PageMsg
  case UpdateTx(value: TxList) extends PageMsg
  case Update1(value: SummaryModel) extends PageMsg
  case Update2(value: BlcList) extends PageMsg
  case Update3(value: TxList) extends PageMsg

  case UpdateTxDetailPage(hash: String) extends PageMsg
  case UpdateTxDetail(v: TxDetail) extends PageMsg

  case UpdateBlcDetailPage(hash: String) extends  PageMsg
  case UpdateBlcDetail(v: BlockDetail) extends  PageMsg

case class PopupMsg(value: Boolean) extends Msg

enum RouterMsg extends Msg:
  case NoOp
  case NavigateTo(page: Page)
  case NavigateToUrl(url: String)
