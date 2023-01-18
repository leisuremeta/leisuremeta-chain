package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    model.curPage match
      case NavMsg.DashBoard =>
        DashboardView.view(model)
      case NavMsg.Blocks =>
        BlocksView.view(model)
      case NavMsg.BlockDetail =>
        BlockDetailView.view(model)
      case NavMsg.Transactions =>
        TransactionsView.view(model)
      case NavMsg.TransactionDetail =>
        TransactionDetailView.view(model)
      case NavMsg.NoPage =>
        NoPageView.view(model)

object PagesView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        SearchView.view(model),
        Pages.render(model),
      ),
    )
