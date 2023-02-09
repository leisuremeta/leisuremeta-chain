package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

// enum PageMsg extends Msg:
//   case PreProcess(search: String)              extends PageMsg
//   case DataProcess(data: String, page: String) extends PageMsg
//   case PageUpdateProcess                       extends PageMsg
//   case PostProcess                             extends PageMsg

// enum NavMsg extends Msg:
//   case DashBoard, Blocks, Transactions, NoPage
//   case TransactionDetail(hash: String) extends NavMsg
//   case BlockDetail(hash: String)       extends NavMsg
//   case NftDetail(hash: String)         extends NavMsg
//   case AccountDetail(hash: String)     extends NavMsg

enum ToggleMsg extends Msg:
  case Click
  case ClickTxDetailInput

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg

enum ApiMsg extends Msg:
  case Update(result: String)    extends ApiMsg
  case GetError(error: String)   extends ApiMsg

enum TxMsg extends Msg:
  case Refresh                  extends TxMsg
  case GetNewTx(result: String) extends TxMsg
  case GetError(error: String)  extends TxMsg

enum TxDetailMsg extends Msg:
  case Patch(hash: String)                              extends TxDetailMsg
  case Update(result: String)                           extends TxDetailMsg
  case GetError(error: String)                          extends TxDetailMsg
  case Get_64Handle_ToBlockDetail(msg: String)          extends TxDetailMsg
  case Get_64Handle_ToTranSactionDetail(result: String) extends TxDetailMsg

enum BlockMsg extends Msg:
  case Refresh                     extends BlockMsg
  case GetNewBlock(result: String) extends BlockMsg
  case GetError(error: String)     extends BlockMsg

enum BlockDetailMsg extends Msg:
  case Patch(hash: String)     extends BlockDetailMsg
  case Update(result: String)  extends BlockDetailMsg
  case GetError(error: String) extends BlockDetailMsg

enum NftDetailMsg extends Msg:
  case Patch(hash: String)     extends NftDetailMsg
  case Update(result: String)  extends NftDetailMsg
  case GetError(error: String) extends NftDetailMsg

enum AccountDetailMsg extends Msg:
  case Patch(hash: String)     extends AccountDetailMsg
  case Update(result: String)  extends AccountDetailMsg
  case GetError(error: String) extends AccountDetailMsg

enum PageMoveMsg extends Msg:
  case Prev                 extends PageMoveMsg
  case Next                 extends PageMoveMsg
  case Get(value: String)   extends PageMoveMsg
  case Patch(value: String) extends PageMoveMsg

// enum DashboardMsg extends Msg:
//   case GetNew(result: String)  extends DashboardMsg
//   case GetError(error: String) extends DashboardMsg
