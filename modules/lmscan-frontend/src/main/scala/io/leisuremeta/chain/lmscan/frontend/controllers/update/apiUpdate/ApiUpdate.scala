// Just sample code
package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object ApiUpdate:
  def update(model: Model): ApiMsg => (Model, Cmd[IO, Msg]) =
    case ApiMsg.Update(r) =>
      (
        model.copy(apiData = Some(r)),
        Cmd.None,
      )
    case ApiMsg.GetError(_) =>
      log("ApiMsg.GetError")
      log((model, Cmd.None))
