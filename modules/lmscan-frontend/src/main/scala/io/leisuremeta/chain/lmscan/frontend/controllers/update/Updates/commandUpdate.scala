package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import org.scalajs.dom.window
import io.leisuremeta.chain.lmscan.frontend.Init.setMode

object CommandUpdate:
  def update(model: Model): CommandMsg => (Model, Cmd[IO, Msg]) =
    case CommandMsg.OnClick(msg) =>
      msg match
        case m: CommandCaseMode =>
          (
            model.copy(
              commandMode = setMode(m),
            ),
            Cmd.None,
          )
        case m: CommandCaseLink =>
          (
            model.copy(
              commandLink = m,
            ),
            Cmd.emit(
              PageMsg.PreUpdate(DashBoard()),
            ),
          )
