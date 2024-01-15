package io.leisuremeta.chain.lmscan
package frontend
package chart

import scala.scalajs.js.Date
import common.model._
import typings.toastUiChart.mod.ColumnLineChart

object AcChart:
  def draw(data: SummaryChart): Option[ColumnLineChart] =
    val list = data.list
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
