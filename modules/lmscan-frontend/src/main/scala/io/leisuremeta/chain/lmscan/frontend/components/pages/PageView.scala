package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Pages:
  def render(model: Model): Html[Msg] =
    log("Builder.getPage(model.observers, model.observerNumber)")
    log(Builder.getPage(model.observers, model.observerNumber))
    Builder.getPage(model.observers, model.observerNumber) match

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
