package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object DashboardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "color-white")(
      "대시보드",
      // BoardView.view(model),
      // CommonTableView.view(model),
    )
