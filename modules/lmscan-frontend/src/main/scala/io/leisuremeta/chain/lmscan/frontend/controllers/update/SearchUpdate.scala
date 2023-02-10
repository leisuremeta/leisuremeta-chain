package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object SearchUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)

    case InputMsg.Patch =>
      val hash = model.searchValue

      (
        model,
        Cmd.emit(PageMsg.PreUpdate(CustomMap.getPageString(model.searchValue))),
      )
