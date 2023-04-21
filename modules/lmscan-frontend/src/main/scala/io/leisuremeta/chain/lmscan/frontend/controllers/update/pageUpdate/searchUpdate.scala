package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object SearchUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)

    case InputMsg.Patch =>
      (
        model.copy(searchValue = ""),
        Cmd.emit(
          PageMsg.PreUpdate(ValidPageName.getPageString(model.searchValue)),
        ),
      )
