package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    Builder.getPage(model.observers, model.observers.length) match

      case PageCase.Observer(_, _) =>
        ObserverView.view(model)

      case PageCase.DashBoard(_, _) =>
        div()(
          ObserverView.view(model),
          DashboardView.view(model),
        )

      case PageCase.Blocks(_, _) =>
        ObserverView.view(model)

      case PageCase.Transactions(_, _) =>
        ObserverView.view(model)

      case _ => div("매칭x")

object PageView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        div(`class` := "pb-32px")(Pages.render(model)),
      ),
    )
