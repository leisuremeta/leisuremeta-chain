package io.leisuremeta.chain.lmscan
package frontend
package chart

import tyrian.Html.*
import tyrian.*
import common.model._
import java.time.Instant
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.JSConverters.*

object BalChart {
  def view(model: BaseModel): Html[Msg] =
    renderDataChart(model.chartData)
    div(
      id := "chart",
    )("")

  def toVal(b: Option[BigDecimal]): Double =
    val res = for
      x <- b
      y = x / BigDecimal("1e+18")
      z = y.toDouble
    yield z
    res.getOrElse(0.0)

  def toX(t: Option[Long]): String =
    val res = for
      x <- t
      y = Instant.ofEpochSecond(x).toString.dropRight(1).split("T").mkString(s"\n")
    yield y
    res.getOrElse("")

  def renderDataChart(data: SummaryChart): Unit =
    data.list.reverse match
      case List() => ()
      case list => 
        val label: Seq[String] = list.map(b => toX(b.createdAt))
        val arr: Seq[Double] = list.map(b => toVal(b.totalBalance))
        ChartHandler.drawBal(label, arr) 
}
