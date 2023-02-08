package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    // 1 prevPage, searchvaluestore
    case PageMsg.PreProcess(search: String) =>
      log(
        (
          model.copy(prevPage = model.curPage, searchValueStore = search),
          Cmd.Emit(PageMsg.DataProcess),
        ),
      )

    // 2
    case PageMsg.DataProcess =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )

    // 3
    case PageMsg.PageUpdateProcess =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )

    // 4
    case PageMsg.PostProcess =>
      log(
        (
          model.copy(curPage = NavMsg.DashBoard),
          Cmd.None,
        ),
      )
