package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object CommonTableView:
  def view(model: Model): Html[Msg] =
    div()
    // find_current_PageCase(model) match
    //   case Transactions(_, _, _, _) =>
    //     div(`class` := "table-area")(
    //       div(`class` := "font-40px pt-16px font-block-detail color-white")(
    //         "Transactions",
    //       ),
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         TransactionTable.view(model),
    //       ),
    //     )

    //   case BlockDetail(_, _, _, _) =>
    //     div(`class` := "table-area ")(
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         TransactionTable.view(model),
    //       ),
    //     )

    //   case TxDetail(_, _, _, _) =>
    //     div(`class` := "table-area ")(
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         TransactionTable.view(model),
    //       ),
    //     )

    //   case AccountDetail(_, _, _, _) =>
    //     div(`class` := "table-area ")(
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         TransactionTable.view(model),
    //       ),
    //     )
    //   case NftDetail(_, _, _, _) =>
    //     div(`class` := "table-area ")(
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         TransactionTable.view(model),
    //       ),
    //     )
    //   case Observer(_, _, _, _) =>
    //     div(`class` := "table-area ")(
    //       div(id := "oop-table-blocks", `class` := "table-list x")(
    //         ObserverView.view(model),
    //       ),
    //     )

    //   case NoPage(_, _, _, _) =>
    //     div()
