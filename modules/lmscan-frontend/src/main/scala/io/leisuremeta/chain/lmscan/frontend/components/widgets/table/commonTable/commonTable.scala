package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object CommonTableView:
  def view(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case DashBoard(_, _, _, _) =>
        div(`class` := "table-area")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
            TransactionTable.view(model),
          ),
        )

      case Blocks(_, _, _, _) =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail color-white")(
            "Blocks",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            BlockTable.view(model),
          ),
        )

      case Transactions(_, _, _, _) =>
        div(`class` := "table-area")(
          div(`class` := "font-40px pt-16px font-block-detail color-white")(
            "Transactions",
          ),
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case BlockDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case TxDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )

      case AccountDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )
      case NftDetail(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            TransactionTable.view(model),
          ),
        )
      case Observer(_, _, _, _) =>
        div(`class` := "table-area ")(
          div(id := "oop-table-blocks", `class` := "table-list x")(
            ObserverView.view(model),
          ),
        )

      case NoPage(_, _, _, _) =>
        div()
