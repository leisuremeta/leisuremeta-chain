package io.leisuremeta.chain.lmscan.frontend
// import tyrian.*
// import cats.effect.IO
sealed trait Msg

enum NavMsg extends Msg:
  case DashBoard, Blocks, Transactions

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg
  case Patch              extends InputMsg
