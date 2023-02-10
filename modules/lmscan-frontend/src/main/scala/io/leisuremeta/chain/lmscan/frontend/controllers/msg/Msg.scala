package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

enum PageMsg extends Msg:
  case PreUpdate(search: PageName)              extends PageMsg
  case DataUpdate(data: String, page: PageName) extends PageMsg
  case PageUpdate                               extends PageMsg
  case PostUpdate                               extends PageMsg
  case GetError(msg: String, page: PageName)    extends PageMsg

enum ToggleMsg extends Msg:
  case Click
  case ClickTxDetailInput

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg

enum ApiMsg extends Msg:
  case Update(result: String)  extends ApiMsg
  case GetError(error: String) extends ApiMsg

enum PageMoveMsg extends Msg:
  case Prev                 extends PageMoveMsg
  case Next                 extends PageMoveMsg
  case Get(value: String)   extends PageMoveMsg
  case Patch(value: String) extends PageMoveMsg

// No more use ========================================================
// enum TxMsg extends Msg:
//   case Refresh                  extends TxMsg
//   case GetNewTx(result: String) extends TxMsg
//   case GetError(error: String)  extends TxMsg

// enum TxDetailMsg extends Msg:
//   case Patch(hash: String)                              extends TxDetailMsg
//   case Update(result: String)                           extends TxDetailMsg
//   case GetError(error: String)                          extends TxDetailMsg
//   case Get_64Handle_ToBlockDetail(msg: String)          extends TxDetailMsg
//   case Get_64Handle_ToTranSactionDetail(result: String) extends TxDetailMsg

// enum BlockMsg extends Msg:
//   case Refresh                     extends BlockMsg
//   case GetNewBlock(result: String) extends BlockMsg
//   case GetError(error: String)     extends BlockMsg

// enum BlockDetailMsg extends Msg:
//   case Patch(hash: String)     extends BlockDetailMsg
//   case Update(result: String)  extends BlockDetailMsg
//   case GetError(error: String) extends BlockDetailMsg

// enum NftDetailMsg extends Msg:
//   case Patch(hash: String)     extends NftDetailMsg
//   case Update(result: String)  extends NftDetailMsg
//   case GetError(error: String) extends NftDetailMsg

// enum AccountDetailMsg extends Msg:
//   case Patch(hash: String)     extends AccountDetailMsg
//   case Update(result: String)  extends AccountDetailMsg
//   case GetError(error: String) extends AccountDetailMsg

// enum DashboardMsg extends Msg:
//   case GetNew(result: String)  extends DashboardMsg
//   case GetError(error: String) extends DashboardMsg
