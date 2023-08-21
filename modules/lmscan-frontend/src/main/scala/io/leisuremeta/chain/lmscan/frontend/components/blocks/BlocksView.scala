package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlocksView:
  def view(model: Model): Html[Msg] =
    CommonTableView.view(model)
