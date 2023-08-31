package io.leisuremeta.chain.lmscan.frontend.chart

import typings.chartJs.mod.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.JSConverters.*

object ChartConfig:
  def config(labelList: List[String], gData: List[Double], labelName: String) =
    new ChartConfiguration {
      `type` = "line"
      data = new ChartData {
        labels = labelList.toJSArray
        datasets = js.Array(
          new ChartDataSets {
            label = labelName
            borderWidth = 3
            borderColor = "rgba(81,255,0,0.72)"
            data = gData.toJSArray
          },
        )
      }
      options = new ChartOptions {
        legend = new ChartLegendOptions {
          display = false
        }
        scales = new ChartScales {
          yAxes = js.Array(new CommonAxe {
            ticks = new TickOptions {
            }
          })
        }
      }
    }
