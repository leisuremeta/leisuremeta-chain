package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}

import Log.*

object TransactionTable:
  def view(model: Model): Html[Msg] =
    model.curPage match
      case PageName.BlockDetail(_) =>
        div(`class` := "table-container")(
          Table.blockDetail_txtable(model),
        )

      case PageName.AccountDetail(_) =>
        div(`class` := "table-container")(
          Table.accountDetail_txtable(model),
        )

      case PageName.DashBoard =>
        div(`class` := "table-container")(
          Title.tx(model),
          Table.dashboard_txtable(model),
        )
      case PageName.Transactions =>
        div(`class` := "table-container")(
          Table.txList_txtable(model),
        )

      case _ =>
        div(`class` := "table-container")(
          Table.blockDetail_txtable(model),
        )
