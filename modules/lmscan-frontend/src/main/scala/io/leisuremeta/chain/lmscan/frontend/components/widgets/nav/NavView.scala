package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

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
          `class` := s"${PageName.Observer
              .toString() == model.observer.takeRight(1)(0).toString()}",
          onClick(PageMsg.PreUpdate(PageName.Observer)),
        )(
          span()(
            PageName.Observer.toString(),
          ),
        ),
        button(
          `class` := s"${PageName.DashBoard
              .toString() == model.observer.takeRight(1)(0).toString()}",
          onClick(PageMsg.PreUpdate(PageName.DashBoard)),
        )(span()(PageName.DashBoard.toString())),
      ),
    )
