package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.syntax.*

object AccountView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px color-white")(
        "Account",
      ),
      div(`class` := "x")(CommonDetailTable.view(model)),
      div(`class` := "font-40px pt-32px font-block-detail pb-16px color-white")(
        "Transaction History",
      ),
      CommonTableView.view(model),
    )
