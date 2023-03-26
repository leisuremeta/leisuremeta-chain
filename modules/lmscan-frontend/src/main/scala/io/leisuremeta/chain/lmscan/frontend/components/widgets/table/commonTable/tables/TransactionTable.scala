package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import Log.*

object TransactionTable:
  def view(model: Model): Html[Msg] =
    get_PageCase(model) match
      case PageCase.Transactions(_, _, _, _) =>
        div(`class` := "table-container")(
          Table.txList_txtable(model),
          Search.search_tx(model),
        )
      case PageCase.DashBoard(_, _, _, _) =>
        div(`class` := "table-container")(
          Title.tx(model),
          Table.dashboard_txtable(model),
          // Search.search_tx(model),
        )

      case PageCase.BlockDetail(_, _, _, _) =>
        div(`class` := "table-container")(
          Table.blockDetail_txtable(model),
          // Search.search_tx(model),
        )

      case PageCase.AccountDetail(_, _, _, _) =>
        div(`class` := "table-container")(
          Table.accountDetail_txtable(model),
          Search.search_tx(model),
        )

      //   case PageName.NftDetail(_) =>
      //     div(`class` := "table-container")(
      //       Table.nftDetail_txtable(model),
      //       // Search.search_tx(model),
      //     )

      case _ => div()
      // div(`class` := "table-container")(
      //   Table.blockDetail_txtable(model),
      // )
