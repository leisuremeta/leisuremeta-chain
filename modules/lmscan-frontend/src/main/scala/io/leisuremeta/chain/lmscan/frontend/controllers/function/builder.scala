package io.leisuremeta.chain.lmscan.frontend
import org.scalajs.dom.window
import scala.scalajs.js.{Date, Math}
import scala.concurrent.duration.*

object Builder:
  def getPage(observers: List[Observer]) =
    observers.takeRight(1)(0).pageName.toString()
