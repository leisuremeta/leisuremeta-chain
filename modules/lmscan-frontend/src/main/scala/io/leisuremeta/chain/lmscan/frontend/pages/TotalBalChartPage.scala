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
        Cmd.Emit(UpdateChart),
        Nav.pushUrl("chart/balance")
      )
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "color-white")(
        BalChart.view(model)
      )
    )
