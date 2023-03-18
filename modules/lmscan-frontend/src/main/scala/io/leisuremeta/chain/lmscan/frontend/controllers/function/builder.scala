package io.leisuremeta.chain.lmscan.frontend
import org.scalajs.dom.window
import scala.scalajs.js.{Date, Math}
import scala.concurrent.duration.*

object Builder:
  def getPage(observers: List[ObserverState]) =
    // 최신 상태에서 page 를 만듦
    observers.takeRight(1)(0).pageName.name
