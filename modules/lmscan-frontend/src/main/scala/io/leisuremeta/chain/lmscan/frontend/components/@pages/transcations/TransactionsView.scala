package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object TransactionsView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "font-40px pt-16px font-block-detail")(
        "Transactions",
      ),
      div(id := "oop-table-blocks", `class` := "table-list x")(
        TableTransactionsView.view(model),
      ),
    )
