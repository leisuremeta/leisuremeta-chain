package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._
import chart.AcChart
import typings.toastUiChart.mod.ColumnLineChart

object TotalAccChart:
  def update(model: AccChartModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init =>
      (model, Cmd.Batch(
        DataProcess.getData(SummaryChart()),
        Nav.pushUrl(model.url),
      ))
    case UpdateModel(v: SummaryChart) => (model.copy(chartData = Some(v)), Cmd.Emit(DrawChart))
    case DrawChart => 
      val chart = model.chartData match
        case Some(data) => AcChart.draw(data)
        case _ => None
      (model.copy(chart = chart), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => 
      model.chart match 
        case Some(c) => 
          c.destroy()
          (model.copy(chart = None), Cmd.emit(msg))
        case _ =>
          (model.toEmptyModel, Cmd.emit(msg))

  def view(model: AccChartModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "chart-wrap color-white")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Daily Transactions",
        ),
        div(id := "chart")(""),
      )
    )

final case class AccChartModel(
    global: GlobalModel = GlobalModel(),
    chartData: Option[SummaryChart] = None,
    chart: Option[ColumnLineChart] = None,
) extends Model:
    def view: Html[Msg] = TotalAccChart.view(this)
    def url = "/chart/acc"
    def update: Msg => (Model, Cmd[IO, Msg]) = TotalAccChart.update(this)
