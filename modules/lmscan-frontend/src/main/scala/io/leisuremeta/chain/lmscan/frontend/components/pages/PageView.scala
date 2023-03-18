package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    model.curPage match
      case PageName.DashBoard =>
        DashboardView.view(model)

      case PageName.Observer =>
        ObserverView.view(model)

      case _ => div("매칭x")

object PageView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        div(`class` := "pb-32px")(Pages.render(model)),
      ),
    )
