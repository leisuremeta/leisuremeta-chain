package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object PageMoveUpdate:
  def update(model: Model): PageMoveMsg => (Model, Cmd[IO, Msg]) =
    case PageMoveMsg.Next =>
      log((model, Cmd.None))
    case PageMoveMsg.Prev =>
      log((model, Cmd.None))
