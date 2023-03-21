package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import scala.scalajs.js
import Log.*
import org.scalajs.dom.window
object Init:

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (
      Model(
        observers = List(
          ObserverState(
            pageCase = PageCase.Blocks(),
            number = 1,
          ),
        ),
        observerNumber = 1,
      ),
      Cmd.None,
    )
