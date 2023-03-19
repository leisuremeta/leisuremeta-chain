package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Builder.getPage
// import PageCase
object NavView:

  def view(model: Model): Html[Msg] =
    nav(`class` := "")(
      div(id := "title", onClick(PageMsg.PreUpdate(PageCase.DashBoard())))(
        span(id := "head")(img(id := "head-logo")),
      ),
      div(
        id := "buttons",
      )(
        button(
          `class` := s"${PageCase.Observer() == getPage(model.observers, model.observerNumber)}",
          onClick(PageMsg.PreUpdate(PageCase.Observer())),
        )(
          span()(
            PageCase.Observer.toString(),
          ),
        ),
        button(
          `class` := s"${PageCase.DashBoard() == getPage(model.observers, model.observerNumber)}",
          onClick(PageMsg.PreUpdate(PageCase.DashBoard())),
        )(span()(PageCase.DashBoard().name)),
        button(
          `class` := s"${PageCase.Blocks() == getPage(model.observers, model.observerNumber)}",
          onClick(PageMsg.PreUpdate(PageCase.Blocks())),
        )(span()(PageCase.Blocks().name)),
        button(
          `class` := s"${PageCase.Transactions() == getPage(model.observers, model.observerNumber)}",
          onClick(PageMsg.PreUpdate(PageCase.Transactions())),
        )(span()(PageCase.Transactions().name)),
      ),
    )
