package io.leisuremeta.chain.lmscan.frontend
import org.scalajs.dom.window
import scala.scalajs.js.{Date}

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

  // [timestamp] => YYYY-MM-DD TIME
  //  1673939878 =>  1970-01-20 08:58:59
