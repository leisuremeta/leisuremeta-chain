package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object Init:
  val page   = NavMsg.TransactionDetail
  val toggle = true
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(page, page, "", toggle), Cmd.None)
