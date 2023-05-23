package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
object Pages:
  def render(model: Model): Html[Msg] =
    find_current_PageCase(model) match
      case PageCase.Observer(_, _, _, _) =>
        ObserverView.view(model)

      case PageCase.DashBoard(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          DashboardView.view(model),
        )

      case PageCase.Blocks(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          BlocksView.view(model),
        )

      case PageCase.Transactions(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          TransactionsView.view(model),
        )

      case PageCase.BlockDetail(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          BlockDetailView.view(model),
        )

      case PageCase.TxDetail(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          TransactionDetailView.view(model),
        )

      case PageCase.AccountDetail(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          AccountView.view(model),
        )

      case PageCase.NftDetail(_, _, _, _) =>
        div()(
          // ObserverView.view(model),
          NftView.view(model),
        )

      case PageCase.NoPage(_, _, _, _) =>
        NoPageView.view(model)

object JsonPages:
  def render(model: Model): Html[Msg] =
    find_current_PageCase(model) match

      case PageCase.NoPage(_, _, _, _) =>
        NoPageView.view(model)

      case _ =>
        JsonView.view(model)

object PageView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        div(`class` := "x")(
          SearchView.view(model),
          model.commandMode match
            case CommandCaseMode.Development => Toggle.view(model)
            case CommandCaseMode.Production  => div(),
        ), {
          model.toggle match
            case true  => div(`class` := "pb-32px")(JsonPages.render(model))
            case false => div(`class` := "pb-32px")(Pages.render(model))
        },
      ),
      PopupView.view(model),
    )
