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
          `class` := s"${PageName.Observer.toString() == model.curPage.toString()}",
          onClick(PageMsg.PreUpdate(PageName.Observer)),
        )(span()("Observer")),
        button(
          `class` := s"${PageName.DashBoard.toString() == model.curPage.toString()}",
          onClick(PageMsg.PreUpdate(PageName.DashBoard)),
        )(span()("Dashboard")),
      ),
    )
