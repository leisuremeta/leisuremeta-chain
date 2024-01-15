package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._
import chart.BalChart
import typings.toastUiChart.mod.LineChart

object TotalBalChart:
  def update(model: BalChartModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init =>
      (model, Cmd.Batch(
        DataProcess.getData(SummaryChart()),
        Nav.pushUrl(model.url),
      ))
    case UpdateModel(v: SummaryChart) => (model.copy(chartData = Some(v)), Cmd.Emit(DrawChart))
    case DrawChart => 
      val chart = model.chartData match
        case Some(data) => BalChart.draw(data)
        case _ => None
      (model.copy(chart = chart), Cmd.None)
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => 
      model.chart match 
        case Some(c) => 
          c.destroy()
          (model.copy(chart = None), Cmd.emit(msg))
        case _ =>
          (model.toEmptyModel, Cmd.emit(msg))

  def view(model: BalChartModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "chart-wrap color-white")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Total Balance",
        ),
        div(id := "chart")(""),
      )
    )

final case class BalChartModel(
    global: GlobalModel = GlobalModel(),
    chartData: Option[SummaryChart] = None,
    chart: Option[LineChart] = None,
) extends Model:
    def view: Html[Msg] = TotalBalChart.view(this)
    def url = "/chart/bal"
    def update: Msg => (Model, Cmd[IO, Msg]) = TotalBalChart.update(this)
