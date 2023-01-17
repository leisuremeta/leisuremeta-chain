package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object BlockDetailView:
  def view(model: Model): Html[Msg] =
    div()(
      div(`class` := "font-40px pt-16px font-block-detail")("block details"),
      div(`class` := "x")(TableDetail.view(model)),
      div(`class` := "table-area")(
        div(id := "oop-table-blocks", `class` := "table-list x")(
          TableBlockView.view(model),
        ),
      ),
    )
