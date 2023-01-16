package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

// object tables:
//   def render(tableSelector: String) => match tableSelector
//     case

object TableView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        TableBlockView.view(model),
        TableTransactionsView.view(model),
      ),
    )
