package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object LoaderView:
  def view: Html[Msg] =
    div(`class` := "loader-case")(
      div(`class` := "loader-container2 xy-center")(
        div(`class` := "loader")(),
      ),
    )
