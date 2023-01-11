package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object SearchView:
  def view(model: Model): Html[Msg] =
    div(`class` := "search")(
      div(id := "")("block number, block hash, account, tx hash"),
      div(
        id := "buttons",
      )("search"),
    )
