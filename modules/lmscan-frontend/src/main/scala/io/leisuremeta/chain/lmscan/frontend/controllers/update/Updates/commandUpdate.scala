package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_Pub_m1s

object CommandUpdate:
  def update(model: Model): CommandMsg => (Model, Cmd[IO, Msg]) =
    case CommandMsg.OnClick(msg) =>
      log((find_current_Pub_m1s(model) ::: List("")).filter(d => d != ""))
      msg match
        case m: CommandCaseMode =>
          (
            model.copy(
              commandMode = m,
            ),
            Cmd.None,
          )
        case m: CommandCaseLink =>
          (
            model.copy(
              commandLink = m,
            ),
            Cmd.emit(
              PageMsg.PreUpdate(PageCase.DashBoard()),
            ),
          )
