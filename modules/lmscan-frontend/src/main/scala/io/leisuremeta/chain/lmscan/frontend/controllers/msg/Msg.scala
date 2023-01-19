package io.leisuremeta.chain.lmscan.frontend
sealed trait Msg

enum NavMsg extends Msg:
  case DashBoard, Blocks, BlockDetail, Transactions, TransactionDetail, NoPage,
    Account, Nft

enum ToggleMsg extends Msg:
  case Click

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg
