package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object InputOutput:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        BlockTable.view(model),
        TransactionTable.view(model),
      ),
    )
