package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object ObserverView:
  def view(model: Model): Html[Msg] =
    div(`class` := "color-white")(
      "옵져버",
      // BoardView.view(model),
      // CommonTableView.view(model),
    )
