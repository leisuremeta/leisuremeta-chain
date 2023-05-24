package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object PopupUpdate:
  def update(model: Model): PopupMsg => (Model, Cmd[IO, Msg]) =
    case PopupMsg.OnClick(value) =>
      (model.copy(popup = value), Cmd.None)
