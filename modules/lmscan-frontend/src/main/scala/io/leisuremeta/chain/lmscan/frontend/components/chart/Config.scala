package io.leisuremeta.chain.lmscan.frontend.chart

import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.JSConverters.*
import typings.toastUiChart.mod
import typings.toastUiChart.typesChartsMod._
import typings.toastUiChart.typesOptionsMod._
import typings.toastUiChart.anon._
import org.scalajs.dom._

@js.native
@JSImport("@toast-ui/chart/dist/toastui-chart.min.css", JSImport.Namespace)
object ChartCss extends js.Object

object ChartHandler:
  private val css = ChartCss

  def drawBal(label: Seq[String], arr: Seq[Double]) = 
    document.getElementById("chart") match
      case x: HTMLDivElement =>
        val opt = LineChartOptions()
          .setLegend(NormalLegendOptions().setVisible(false))
          .setXAxis(LineTypeXAxisOptions().setLabel(Margin().setFormatter(
            (s, _) => s.split(s"\n")(0)
          )))
          .setYAxis(YAxisOptions().setLabel(Formatter().setFormatter(
            (s, _) => String.format("%,d", s.toInt)
          )))
          .setExportMenu(ExportMenuOptions().setVisible(false))
        mod.LineChart(
          LineChartProps(
            LineSeriesData(
              js.Array(LineSeriesInput(arr.toJSArray, "Balance"))
            ).setCategories(label.toJSArray), x, opt)
        )
      case _ => None

  def drawChart(label: Seq[String], arr: Seq[Double], name: String) = 
    document.getElementById("chart") match
      case x: HTMLDivElement =>
        val opt = ColumnLineChartOptions()
          .setLegend(NormalLegendOptions().setVisible(false))
          .setYAxis(YAxisOptions().setLabel(Formatter().setFormatter(
            (s, _) => String.format("%,d", s.toInt)
          )))
          .setExportMenu(ExportMenuOptions().setVisible(false))
        mod.ColumnLineChart(
          ColumnLineChartProps(
            ColumnLineData(
              label.toJSArray,
              Column(
                js.Array(PickBoxSeriesTypeBoxSerie(arr.toJSArray, name)),
                js.Array()
                // js.Array(PickLineSeriesTypenamedat(arr.toJSArray, "Account"))
              )
            ).setCategories(label.toJSArray), x, opt)
        )
      case _ => None
