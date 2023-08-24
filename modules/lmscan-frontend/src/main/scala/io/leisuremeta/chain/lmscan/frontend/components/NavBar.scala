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
        ("Dashboard", MainPage),
        ("Blocks", BlockPage),
        ("Transactions", TxPage),
      ).map((name, page) =>
        div(
          `class` := "buttons",
        )(
          button(
            `class` := s"${name == model.page.name}",
            onClick(RouterMsg.NavigateTo(page)),
          )(span(name))
        )
      ),
    )
