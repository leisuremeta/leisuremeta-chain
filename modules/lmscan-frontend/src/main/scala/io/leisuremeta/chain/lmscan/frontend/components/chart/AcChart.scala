package io.leisuremeta.chain.lmscan
package frontend
package chart

import scala.scalajs.js.Date
import tyrian.Html.*
import tyrian.*
import common.model.SummaryModel

object AcChart {
  def view(model: Model): Html[Msg] =
    renderDataChart(model.chartData)
    canvas(
      width := "800px",
      height := "600px",
      id := "chart",
    )("")

  def renderDataChart(data: List[SummaryModel]): Unit=
    import typings.chartJs.mod.*
    data match
      case List() => ()
      case _ =>
        val gData = data.map(_.totalAccounts.getOrElse(0L)).map(_.toDouble)
        val label = data.map(_.createdAt.getOrElse(0)).map(_.toString)
        val chart = Chart.apply.newInstance2("chart", ChartConfig.config(label, gData, "accounts"))
}
