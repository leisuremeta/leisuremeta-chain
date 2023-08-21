package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object NavBar:
  def view(model: Model): Html[Msg] =
    nav()(
      div(id := "title", onClick(PageMsg.PreUpdate(DashBoard())))(
        span(id := "head")(img(id := "head-logo")),
      )
      ::
      List(
        DashBoard(),
        Blocks(),
        Transactions(),
      ).map(pc =>
        div(
          `class` := "buttons",
        )(
          button(
            `class` := s"${pc.name == find_name(model)}",
            onClick(PageMsg.PreUpdate(pc)),
          )(span(pc.name))
        )
      ),
    )
