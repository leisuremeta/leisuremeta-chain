package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object DetailButtonUpdate:
  def update(model: Model): DetailButtonMsg => (Model, Cmd[IO, Msg]) =
    case DetailButtonMsg.OnClick(value) =>
      (model.copy(detail_button = !value), Cmd.None)
