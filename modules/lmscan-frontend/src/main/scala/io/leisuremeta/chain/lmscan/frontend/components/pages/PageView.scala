package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Pages:
  def render(model: Model): Html[Msg] =
    model.curPage match
      case PageName.DashBoard =>
        div(`class` := "color-white")("대시보드")

      case PageName.Observer =>
        div(`class` := "color-white")("옵져버")

      case _ => div("매칭x")

object PageView:
  def view(model: Model): Html[Msg] =
    div(id := "page", `class` := "")(
      div()(
        div(`class` := "pb-32px")(Pages.render(model)),
      ),
    )
