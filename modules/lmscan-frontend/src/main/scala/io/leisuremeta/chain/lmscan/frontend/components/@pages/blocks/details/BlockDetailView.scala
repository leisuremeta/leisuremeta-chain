package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object BlockDetailView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px")(
        "Block Details",
      ),
      div(`class` := "x")(TableDetail.view(model)),
      div(`class` := "table-area ")(
        div(id := "oop-table-blocks", `class` := "table-list x")(
          TableBlockView.view(model),
          BlockDetailTable.view(model),
        ),
      ),
    )
