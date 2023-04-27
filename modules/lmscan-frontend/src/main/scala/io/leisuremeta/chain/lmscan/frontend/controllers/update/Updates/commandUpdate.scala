package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_Pub_m1s
// import io.leisuremeta.chain.lmscan.frontend.Model.toggle

object CommandUpdate:
  def update(model: Model): CommandMsg => (Model, Cmd[IO, Msg]) =
    case CommandMsg.OnClick(msg) =>
      log((find_current_Pub_m1s(model) ::: List("")).filter(d => d != ""))
      msg match
        case m: CommandCaseMode =>
          (
            model.copy(
              commandMode = m,
              // toggle = msg match
              //   case CommandCaseMode.Development => true
              //   case CommandCaseMode.Production  => false
              //   case _                           => model.toggle,
              //   commandLink =
            ),
            Cmd.None,
          )
        case m: CommandCaseLink =>
          (
            model.copy(
              commandLink = m,
              // commandMode = msg match
              //   case CommandCaseLink.Development => CommandCaseMode.Development
              //   case CommandCaseLink.Production  => CommandCaseMode.Production
              //   case _                           => model.commandMode,
              // ,
              // toggle = msg match
              //   case CommandCaseLink.Development => true
              //   case CommandCaseLink.Production  => false
              //   case _                           => model.toggle,
              //   commandLink =
            ),
            Cmd.emit(
              PageMsg.PreUpdate(PageCase.DashBoard()),
            ),
          )
