package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlocksView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "font-40px pt-16px font-block-detail")(
        "Blocks",
      ),
      div(id := "oop-table-blocks", `class` := "table-list x")(
        TableBlockView.view(model),
        TableBlockView.view(model),
      ),
    )
