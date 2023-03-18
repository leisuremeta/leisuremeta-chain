package io.leisuremeta.chain.lmscan.frontend

import tyrian.*
import cats.effect.IO

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(_) =>
      (
        model.copy(),
        Cmd.None,
      )
    case _ =>
      (
        model.copy(),
        Cmd.None,
      )
