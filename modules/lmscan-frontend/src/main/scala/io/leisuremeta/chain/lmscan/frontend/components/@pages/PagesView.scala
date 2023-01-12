package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    model.tab match
      case NavMsg.DashBoard =>
        DashboardView.view(model)
      case NavMsg.Blocks =>
        BlocksView.view(model)
      case NavMsg.Transactions =>
        TransactionsView.view(model)

object PagesView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "w-[100%]")(Pages.render(model))