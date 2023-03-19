package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlocksView:
  def view(model: Model): Html[Msg] =
    // update - blocks(data)
    CommonTableView.view(model)
