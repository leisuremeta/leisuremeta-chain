package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO

object PageInit:
  val view = NavMsg.BlockDetail

object Init:
  val page = NavMsg.BlockDetail
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(page, page, ""), Cmd.None)
