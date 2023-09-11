package io.leisuremeta.chain.lmscan
package frontend
package chart

import tyrian.Html.*
import tyrian.*
import common.model._

object BalChart {
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
        val gData = list.map(_.totalBalance.getOrElse(BigDecimal(0))).map(a => a / BigDecimal("1e+18")).map(_.toDouble).toList
        val label = list.map(_.createdAt.getOrElse(0)).map(_.toString).toList
        val chart = Chart.apply.newInstance2("chart", ChartConfig.config(label, gData, "balance"))
}
