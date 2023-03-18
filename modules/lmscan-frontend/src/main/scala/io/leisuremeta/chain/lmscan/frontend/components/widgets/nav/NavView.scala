package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Builder.getPage
object NavView:

  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "title", onClick(PageMsg.PreUpdate(PageName.DashBoard)))(
        span(id := "head")(img(id := "head-logo")),
      ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageName.Observer.toString() == getPage(model.observers)}",
          onClick(PageMsg.PreUpdate(PageName.Observer)),
        )(
          span()(
            PageName.Observer.toString(),
          ),
        ),
        button(
          `class` := s"${PageName.DashBoard.toString() == getPage(model.observers)}",
          onClick(PageMsg.PreUpdate(PageName.DashBoard)),
        )(span()(PageName.DashBoard.toString())),
        button(
          `class` := s"${PageName.Blocks.toString() == getPage(model.observers)}",
          onClick(PageMsg.PreUpdate(PageName.Blocks)),
        )(span()(PageName.Blocks.toString())),
        button(
          `class` := s"${PageName.Transactions.toString() == getPage(model.observers)}",
          onClick(PageMsg.PreUpdate(PageName.Transactions)),
        )(span()(PageName.Transactions.toString())),
      ),
    )
