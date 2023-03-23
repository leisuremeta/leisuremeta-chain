package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object TransactionDetailView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px color-white")(
        "Transaction details",
      ),
      div(`class` := "x")(CommonDetailTable.view(model)),
    )
