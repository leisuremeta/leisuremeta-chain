package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import scala.scalajs.js
import Log.*
import org.scalajs.dom.window
import io.leisuremeta.chain.lmscan.frontend.ValidPageName.*
object Init:

  val setProtocol =
    if window.location.href
        .contains("http:") && !window.location.href.contains("local")
    then window.location.href = window.location.href.replace("http:", "https:")

  val path     = window.location.pathname
  val pageCase = getPageCaseFromUrl(path)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        appStates = List(
          StateCase(
            pageCase = PageCase.DashBoard(),
            number = 1,
          ),
        ),
        pointer = 1,
        searchValue = "",
        toggle = false,
        temp = "",
      ),
      Cmd.Emit(PageMsg.PreUpdate(pageCase)),
    )
