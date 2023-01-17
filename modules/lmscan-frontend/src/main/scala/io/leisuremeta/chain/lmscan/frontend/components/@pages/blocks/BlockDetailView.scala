package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object BlockDetailView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        TableBlockView.view(model),
        TableBlockView.view(model),
      ),
    )
