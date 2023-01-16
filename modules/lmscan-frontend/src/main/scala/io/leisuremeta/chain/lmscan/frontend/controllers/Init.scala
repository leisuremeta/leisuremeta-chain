package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
object Init:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(NavMsg.DashBoard, NavMsg.DashBoard, ""), Cmd.None)
