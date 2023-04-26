package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Toggle:
  def view(model: Model): Html[Msg] =
    div(`class` := "toggle-area xy-center")(
      input(`type` := "checkbox", id := "toggle"),
      label(_for := "toggle")(),
    )
