package io.leisuremeta.chain.lmscan
package frontend

import chart._
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case object TotalBalChart extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => (
      model,
      Cmd.Batch(
        Cmd.Emit(UpdateChartAll),
        Nav.pushUrl("/chart/balance")
      )
    )

  def view(m: Model): Html[Msg] =
    m match
      case model: BaseModel =>
        DefaultLayout.view(
          model,
          div(`class` := "chart-wrap color-white")(
            div(`class` := "font-40px pt-16px font-block-detail color-white")(
              "Total Balance",
            ),
            BalChart.view(model)
          )
        )

  def url = "/chart/bal"
