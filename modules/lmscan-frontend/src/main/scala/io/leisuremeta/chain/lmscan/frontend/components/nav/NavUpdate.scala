package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object NavUpdate:
  def update(model: Model): NavMsg => (Model, Cmd[IO, Msg]) =
    case NavMsg.S1 =>
      println(model)
      (model.copy(tab = Tab.S1), Cmd.None)
    case NavMsg.S2 =>
      println(model)
      (model.copy(tab = Tab.S2), Cmd.None)
    case NavMsg.S3 =>
      println(model)
      (model.copy(tab = Tab.S3), Cmd.None)
