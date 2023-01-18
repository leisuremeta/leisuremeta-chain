package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object TransactionDetailView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px")(
        "Transaction details",
      ),
      div(`class` := "x")(CommonDetailTable.view(model)),
      div(`class` := "table-area ")(
        div(id := "oop-table-blocks", `class` := "table-list x")(
          CommonTransactionTable.view(model),
        ),
      ),
    )