package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object TransactionsView:
  def view(model: Model): Html[Msg] =
    CommonTableView.view(model)
