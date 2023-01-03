package io.leisuremeta.chain.explorer.frontend

import scala.scalajs.js.annotation.*

@JSExportTopLevel("LmScan")
object LmscanFrontendMain:
  @JSExportTopLevel("launchApp")
  def launchApp(url: String, port: String): Unit =
    LmscanFrontendApp.launch("app-container")
