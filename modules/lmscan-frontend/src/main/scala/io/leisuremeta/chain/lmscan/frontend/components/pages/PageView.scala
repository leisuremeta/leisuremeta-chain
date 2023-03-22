package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object Pages:
  def render(model: Model): Html[Msg] =
    getObserver_PageCase(model.observers, model.observerNumber) match

      // case PageCase.Observer(_, _) =>
      //   ObserverView.view(model)

      // case PageCase.DashBoard(_, _) =>
      //   div()(
      //     ObserverView.view(model),
      //     DashboardView.view(model),
      //   )

      case PageCase.Blocks(_, _, _, _) =>
        div()(
          ObserverView.view(model),
          BlocksView.view(model),
        )

      // case PageCase.Transactions(_, _) =>
      //   div()(
      //     ObserverView.view(model),
      //     TransactionsView.view(model),
      //   )

      case _ => div("매칭x")

object PageView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        div(`class` := "pb-32px")(Pages.render(model)),
      ),
    )
