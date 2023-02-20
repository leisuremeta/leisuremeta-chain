package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Title.block(model),
      Table.block(model),
      Search.search(model),
    )
