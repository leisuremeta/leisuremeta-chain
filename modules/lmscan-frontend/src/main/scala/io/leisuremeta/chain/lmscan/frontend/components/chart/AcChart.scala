package io.leisuremeta.chain.lmscan
package frontend
package chart

import scala.scalajs.js.Date
import tyrian.Html.*
import tyrian.*
import common.model._

object AcChart {
  def view(model: Model): Html[Msg] =
    renderDataChart(model.chartData)
    canvas(
      width := "800px",
      height := "600px",
      id := "chart",
    )("")

  def renderDataChart(data: SummaryChart): Unit=
    import typings.chartJs.mod.*
    data.list match
      case List() => ()
      case list =>
        val gData = list.map(_.totalAccounts.getOrElse(0L)).map(_.toDouble).toList
        val label = list.map(_.createdAt.getOrElse(0)).map(_.toString).toList
        val chart = Chart.apply.newInstance2("chart", ChartConfig.config(label, gData, "accounts"))
}
