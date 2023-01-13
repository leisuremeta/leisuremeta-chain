package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object InputUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      println(model.searchValue)
      (model.copy(searchValue = s), Cmd.None)
    case InputMsg.Patch =>
      println(s"InputMsg.Patch:: $model")
      (model, Cmd.None)
