package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object Pages:
  def render(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case _ :Observer =>
        ObserverView.view(model)

      case _ :DashBoard =>
          DashboardView.view(model)

      case _ :Blocks =>
          BlocksView.view(model)

      case _ :Transactions =>
          TransactionsView.view(model)

      case _ :BlockDetail =>
          BlockDetailView.view(model)

      case _ :TxDetail =>
          TransactionDetailView.view(model)

      case _ :AccountDetail =>
          AccountView.view(model)

      case _ :NftDetail =>
          NftView.view(model)

      case _ :NoPage =>
        NoPageView.view(model)

object JsonPages:
  def render(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case _ :NoPage =>
        NoPageView.view(model)

      case _ =>
        JsonView.view(model)

object ContentPage:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div(`class` := "x")(
        SearchView.view(model),
        model.commandMode match
          case CommandCaseMode.Development => Toggle.view(model)
          case CommandCaseMode.Production  => div(),
      ), 
      div(`class` := "pb-32px")(if (model.toggle) JsonPages.render(model) else Pages.render(model)),
      PopupView.view(model),
    )
