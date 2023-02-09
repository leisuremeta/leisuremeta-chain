package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import Log.log
import CustomMap.*

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(search: PageName) =>
      (
        model.copy(
          prevPage = model.curPage,
          searchValueStore = search.toString(),
          pageNameStore = getPage(search),
          urlStore = search.toString(),
        ), {
          List(PageName.DashBoard, PageName.Blocks, PageName.Transactions)
            .contains(getPage(search)) match
            case true  => Cmd.Emit(PageMsg.DataUpdate)
            case false => CommonDataProcess.getData(search)
        },
      )

    // #data update
    case PageMsg.DataUpdate =>
      (
        model.copy(curPage = model.pageNameStore),
        Cmd.None,
      )

    // #page update
    case PageMsg.PageUpdate =>
      (
        model.copy(prevPage = model.pageNameStore),
        Cmd.None,
      )

    case PageMsg.PostUpdate =>
      (
        model.copy(curPage = PageName.DashBoard),
        Cmd.None,
      )
