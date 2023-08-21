package io.leisuremeta.chain.lmscan.frontend

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
          ValidPageName.getPageFromString(model.searchValue) match
            case pagecase: PageCase =>
              PageMsg.PreUpdate(
                pagecase,
              )
            case commandcase: CommandCaseMode =>
              CommandMsg.OnClick(
                commandcase,
              )
            case commandcase: CommandCaseLink =>
              CommandMsg.OnClick(
                commandcase,
              ),
        ),
      )
