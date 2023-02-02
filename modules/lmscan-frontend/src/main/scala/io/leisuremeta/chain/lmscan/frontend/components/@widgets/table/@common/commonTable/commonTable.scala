package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Tables:
  def render(model: Model): Html[Msg] =
    model.curPage match

      case NavMsg.DashBoard =>
        div(`class` := "table-area")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
            TransactionTable.view(model),
          ),
        )

      case NavMsg.Blocks =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail")(
            "Blocks",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
          ),
        )

      case NavMsg.BlockDetail(_) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case NavMsg.Transactions =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail")(
            "Transactions",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case NavMsg.TransactionDetail(_) =>
        TransactionDetailView.view(model)

      case NavMsg.NoPage =>
        NoPageView.view(model)

      case NavMsg.Account =>
        div(`class` := "table-area")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case _ =>
        div(`class` := "hidden")()

object CommonTableView:
  def view(model: Model): Html[Msg] =
    Tables.render(model)
