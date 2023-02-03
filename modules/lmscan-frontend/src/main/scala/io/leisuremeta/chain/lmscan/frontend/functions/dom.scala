package io.leisuremeta.chain.lmscan.frontend
import org.scalajs.dom.window
import scala.scalajs.js.{Date}
import scala.concurrent.duration.*

object Dom:
  // currently only use className selection, but could be update as more generic fuction
  def select(className: String) =
    window.document.getElementsByClassName(s"$className").item(0)

  def _hidden[A](a: A, b: A) = if (a == b) then "hidden" else ""

  def isEqGet[A](a: A, b: A, c: Any) = if (a == b) then s"$c" else ""

  def yyyy_mm_dd_time(timestamp: Int) =
    new Date(timestamp)
      .toISOString()
      .slice(0, 19)
      .replace("T", " ")
    timeAgo(timestamp)

  // [timestamp] => YYYY-MM-DD TIME
  //  1673939878 =>  1970-01-20 08:58:59

  def timeAgo(timestamp: Double) =
    val now     = Date.now()
    val from    = timestamp
    val timeGap = (now - from) / 1000

    val times = List(
      ((timeGap / 31536000).toInt.toString(), "year ago"),
      ((timeGap / 2592000).toInt.toString(), "month ago"),
      ((timeGap / 86400).toInt.toString(), "day ago"),
      ((timeGap / 3600).toInt.toString(), "hour ago"),
      ((timeGap / 60).toInt.toString(), "min ago"),
      ((timeGap / 1).toInt.toString(), "s ago"),
    )

    times
      .filter((time, msg) => !time.startsWith("0"))
      .map((time, msg) =>
        time + {
          // fix plural
          time.toInt > 1 match
            case true  => msg.replace(" ", "s ").replace("ss", "s")
            case false => msg
        },
      )(0)
