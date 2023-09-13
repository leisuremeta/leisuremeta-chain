package io.leisuremeta.chain.lmscan
package frontend
package chart

import scala.scalajs.js.Date
import tyrian.Html.*
import tyrian.*
import common.model._

object AcChart:
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
        val gData = list.sliding(2).map(x => calData(x.head, x.last)).toList.reverse
        val label = List("4 Day", "3 Day", "2 Day", "1 Day", "Today")
        val chart = Chart.apply.newInstance2("chart", ChartConfig.config(
          chartType = "bar",
          labelList = label, 
          gData = gData, 
          labelName = "Accounts",
        ))
  def calData(s: SummaryModel, e: SummaryModel): Double =
    (s.totalAccounts, e.totalAccounts) match
      case (Some(st), Some(et)) => (st - et).toDouble
      case (_, _) => 0.0
    