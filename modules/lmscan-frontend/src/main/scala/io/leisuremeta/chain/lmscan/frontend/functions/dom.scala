package io.leisuremeta.chain.lmscan.frontend
import org.scalajs.dom.window

object Dom:
  // currently only use className selection, but could be update as more generic fuction
  def select(className: String) =
    window.document.getElementsByClassName(s"$className").item(0)

  def _hidden[A](a: A, b: A) = if (a == b) then "hidden" else ""

  def isEqGet[A](a: A, b: A, c: Any) = if (a == b) then s"$c" else ""
