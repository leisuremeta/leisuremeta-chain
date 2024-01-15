package io.leisuremeta.chain.lmscan
package frontend
package chart

import common.model._
import java.time.Instant
import typings.toastUiChart.mod.LineChart

object BalChart:
  def draw(data: SummaryChart): Option[LineChart] =
    val list = data.list
    val label: Seq[String] = list.map(b => toX(b.createdAt))
    val arr: Seq[Double] = list.map(b => toVal(b.totalBalance))
    ChartHandler.drawBal(label, arr) 

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
