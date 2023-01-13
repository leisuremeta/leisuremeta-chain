package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log

object NavUpdate:
  def update(model: Model): NavMsg => (Model, Cmd[IO, Msg]) =
    case NavMsg.DashBoard =>
      (model.copy(tab = NavMsg.DashBoard), Cmd.None)
    case NavMsg.Blocks =>
      (model.copy(tab = NavMsg.Blocks), Cmd.None)
    case NavMsg.Transactions =>
      (model.copy(tab = NavMsg.Transactions), Cmd.None)
