package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log

object NavUpdate:
  def update(model: Model): NavMsg => (Model, Cmd[IO, Msg]) =
    case NavMsg.DashBoard =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.Blocks =>
      log(
        (
          model.copy(curPage = NavMsg.Blocks, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.BlockDetail =>
      log(
        (
          model.copy(curPage = NavMsg.BlockDetail, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.Transactions =>
      log(
        (
          model.copy(curPage = NavMsg.Transactions, prevPage = model.curPage),
          Cmd.None,
        ),
      )
    case NavMsg.NoPage =>
      log(
        (
          model.copy(curPage = NavMsg.NoPage, prevPage = model.prevPage),
          Cmd.None,
        ),
      )
