package io.leisuremeta.chain.lmscan.frontend

import tyrian.*
import cats.effect.IO

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(page: PageName) =>
      (
        model.copy(curPage = page),
        Cmd.None,
      )
    case _ =>
      (
        model.copy(),
        Cmd.None,
      )
