package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object ToggleUpdate:
  def update(model: Model): ToggleMsg => (Model, Cmd[IO, Msg]) =
    case ToggleMsg.Click =>
      log((model.copy(toggle = !model.toggle), Cmd.None))
    case ToggleMsg.ClickTxDetailInput =>
      (model.copy(toggleTxDetailInput = !model.toggleTxDetailInput), Cmd.None)
