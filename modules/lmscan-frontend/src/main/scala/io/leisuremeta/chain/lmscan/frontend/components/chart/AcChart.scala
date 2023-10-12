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
    div(id := "chart")("")

  def renderDataChart(data: SummaryChart): Unit = 
    data.list match
      case List() => ()
      case list =>
        val arr = list.sliding(2).map(x => calData(x.head, x.last)).toList.reverse
        val label = calLabel(arr.length)
        ChartHandler.drawChart(label, arr, "Account")

  def calData(s: SummaryModel, e: SummaryModel): Double =
    (s.totalAccounts, e.totalAccounts) match
      case (Some(st), Some(et)) => (st - et).toDouble
      case (_, _) => 0.0
    
  def calLabel(n: Int): List[String] =
    if (n == 1) List("Today")
    else s"${n - 1} Day" :: calLabel(n - 1)
