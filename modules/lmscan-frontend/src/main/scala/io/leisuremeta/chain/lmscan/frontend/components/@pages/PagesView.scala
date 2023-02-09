package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    model.curPage match
      case PageName.DashBoard =>
        DashboardView.view(model)
      case PageName.Blocks =>
        BlocksView.view(model)
      case PageName.BlockDetail(_) =>
        BlockDetailView.view(model)
      case PageName.Transactions =>
        TransactionsView.view(model)
      case PageName.TransactionDetail(_) =>
        TransactionDetailView.view(model)
      case PageName.NoPage =>
        NoPageView.view(model)
      case PageName.AccountDetail(_) =>
        AccountView.view(model)
      case PageName.NftDetail(_) =>
        NftView.view(model)

object PagesView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        SearchView.view(model),
        div(`class` := "pb-32px")(Pages.render(model)),
      ),
    )
