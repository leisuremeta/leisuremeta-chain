package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object DashboardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "")(
      BoardView.view(model),
      CommonTableView.view(model),
    )
