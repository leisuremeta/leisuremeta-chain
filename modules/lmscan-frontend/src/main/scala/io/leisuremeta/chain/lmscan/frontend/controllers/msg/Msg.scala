package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

enum NavMsg extends Msg:
  case DashBoard, Blocks, BlockDetail, Transactions, TransactionDetail, NoPage,
    Account, Nft

enum ToggleMsg extends Msg:
  case Click

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg

enum ApiMsg extends Msg:
  case Refresh                   extends ApiMsg
  case GetNewGif(result: String) extends ApiMsg
  case GetError(error: String)   extends ApiMsg

enum TxMsg extends Msg:
  case Refresh                  extends TxMsg
  case GetNewTx(result: String) extends TxMsg
  case GetError(error: String)  extends TxMsg

enum PageMoveMsg extends Msg:
  case Prev                 extends PageMoveMsg
  case Next                 extends PageMoveMsg
  case Get(value: String)   extends PageMoveMsg
  case Patch(value: String) extends PageMoveMsg
enum DashboardMsg extends Msg:
  case GetNew(result: String)  extends DashboardMsg
  case GetError(error: String) extends DashboardMsg
