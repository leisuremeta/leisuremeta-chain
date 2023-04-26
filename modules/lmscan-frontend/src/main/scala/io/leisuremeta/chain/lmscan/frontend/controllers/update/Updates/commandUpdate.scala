package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_Pub_m1s
// import io.leisuremeta.chain.lmscan.frontend.Model.toggle

object CommandUpdate:
  def update(model: Model): CommandMsg => (Model, Cmd[IO, Msg]) =
    case CommandMsg.OnClick(msg: CommandCase) =>
      log((find_current_Pub_m1s(model) ::: List("")).filter(d => d != ""))
      (
        model.copy(
          command = msg,
          toggle = msg match
            case CommandCase.Development => true
            case CommandCase.Production  => false,
        ),
        Cmd.None,
      )
