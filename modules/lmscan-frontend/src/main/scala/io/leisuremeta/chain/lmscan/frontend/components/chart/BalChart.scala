package io.leisuremeta.chain.lmscan
package frontend
package chart

import tyrian.Html.*
import tyrian.*
import common.model.SummaryModel

object BalChart {
  def view(model: Model): Html[Msg] =
    renderDataChart(model.chartData)
    div(
      canvas(
        width := "800px",
        height := "600px",
        id := "chart",
      )("")
    )  

  def renderDataChart(data: List[SummaryModel]): Unit=
    import typings.chartJs.mod.*
    data match
      case List() => ()
      case _ =>
        val gData = data.map(_.total_balance.getOrElse(BigDecimal(0))).map(a => a / BigDecimal("1e+18")).map(_.toDouble)
        val label = data.map(_.createdAt.getOrElse(0)).map(_.toString)
        val chart = Chart.apply.newInstance2("chart", ChartConfig.config(label, gData, "balance"))
}
