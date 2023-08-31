package io.leisuremeta.chain.lmscan
package frontend

import chart._
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case object TotalAcChart extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => (
      model,
      Cmd.Batch(
        Cmd.Emit(UpdateChart),
        Nav.pushUrl("/chart/account")
      )
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "chart-wrap color-white")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Account Chart",
        ),
        AcChart.view(model)
      )
    )
