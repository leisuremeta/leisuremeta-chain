package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import scala.scalajs.js
import Log.*
import org.scalajs.dom.window
object Init:

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    log(Fx.view)
    (
      Model(
        appStates = List(
          StateCase(
            pageCase = PageCase.DashBoard(),
            number = 1,
          ),
        ),
        pointer = 1,
      ),
      // Batch()
      Cmd.Emit(PageMsg.PreUpdate(PageCase.DashBoard())),
    )
