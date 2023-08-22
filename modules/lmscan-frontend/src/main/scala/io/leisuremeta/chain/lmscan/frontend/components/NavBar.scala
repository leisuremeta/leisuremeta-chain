package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NavBar:
  def view(model: Model): Html[Msg] =
    nav()(
      div(id := "title", onClick(RouterMsg.NavigateTo(MainPage)))(
        span(id := "head")(img(id := "head-logo")),
      )
      ::
      List(
        MainPage,
        BlockPage,
        // Transactions(),
      ).map(page =>
        div(
          `class` := "buttons",
        )(
          button(
            `class` := s"${page.name == model.page}",
            onClick(RouterMsg.NavigateTo(page)),
          )(span(page.name))
        )
      ),
    )
