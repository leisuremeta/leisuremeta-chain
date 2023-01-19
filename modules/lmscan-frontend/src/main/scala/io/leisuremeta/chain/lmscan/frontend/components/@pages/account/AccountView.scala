package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object AccountView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px")(
        "Account",
      ),
      div(`class` := "x")(CommonDetailTable.view(model)),
      div(`class` := "font-40px pt-32px font-block-detail pb-16px")(
        "Transaction History",
      ),
      div(`class` := "table-area")(
        div(id := "oop-table-blocks", `class` := "table-list x")(
          CommonTransactionTable.view(model),
        ),
      ),
    )
