package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_Pub_m1s

object ToggleUpdate:
  def update(model: Model): ToggleMsg => (Model, Cmd[IO, Msg]) =
    case ToggleMsg.OnClick(value) =>
      log((find_current_Pub_m1s(model) ::: List("")).filter(d => d != ""))
      (model.copy(toggle = !value), Cmd.None)
