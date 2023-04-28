package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_Pub_m1s
// import io.leisuremeta.chain.lmscan.frontend.Model.detail_button

object DetailButtonUpdate:
  def update(model: Model): DetailButtonMsg => (Model, Cmd[IO, Msg]) =
    case DetailButtonMsg.OnClick(value) =>
      (model.copy(detail_button = !value), Cmd.None)
