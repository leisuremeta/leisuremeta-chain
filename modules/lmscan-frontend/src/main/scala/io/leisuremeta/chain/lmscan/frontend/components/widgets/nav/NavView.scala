package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
object NavView:

  // def isCurPageisDashBoard = (model: Model) =>
  // DashBoardPageList.contains(model.curPage.toString())

  // def isCurPageisBlock = (model: Model) =>
  //   BlockPageList
  //     .reduce((a, b) => a + b)
  //     .contains(Log.log(model.curPage.toString().take(5))) // TODO :: A

  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "title", onClick(PageMsg.PreUpdate(PageCase.DashBoard())))(
        span(id := "head")(img(id := "head-logo")),
      ),
      // div(
      //   id := "buttons",
      // )(
      //   button(
      //     `class` := s"${PageCase.Observer().name == find_name(model)}",
      //     onClick(PageMsg.PreUpdate(PageCase.Observer())),
      //   )(span()(PageCase.Observer().name)),
      // ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageCase.DashBoard().name == find_name(model)}",
          onClick(PageMsg.PreUpdate(PageCase.DashBoard())),
        )(span()(PageCase.DashBoard().name)),
      ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageCase.Blocks().name == find_name(model)}",
          onClick(PageMsg.PreUpdate(PageCase.Blocks())),
        )(span()(PageCase.Blocks().name)),
      ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageCase.Transactions().name == find_name(model)}",
          onClick(PageMsg.PreUpdate(PageCase.Transactions())),
        )(span()(PageCase.Transactions().name)),
      ),
    )
