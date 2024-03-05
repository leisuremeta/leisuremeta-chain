package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object LoaderView:
  def view: Html[Msg] =
    div(cls := "loader-case")(
      div(cls := "loader")(),
    )
