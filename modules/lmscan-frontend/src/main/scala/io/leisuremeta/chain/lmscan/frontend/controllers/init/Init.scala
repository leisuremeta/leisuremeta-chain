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

  def setMode(mode: CommandCaseMode) =
    window.localStorage.setItem("commandMode", mode.toString())
    window.localStorage.setItem("limit", "50000")
    mode

  val path = window.location.pathname

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
        commandMode =
          window.location.href.contains("https://scan.leisuremeta.io") match
            case true =>
              setMode(CommandCaseMode.Production)
            case _ =>
              setMode(CommandCaseMode.Development)
        ,
        commandLink =
          window.location.href.contains("https://scan.leisuremeta.io") match
            case true => CommandCaseLink.Production
            case _    => CommandCaseLink.Development,
      ),
      Cmd.Batch(
        Cmd.Emit(PageMsg.PreUpdate(pageCase)),
      ),
    )
