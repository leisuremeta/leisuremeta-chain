// Just sample code
package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object ApiUpdate:
  def update(model: Model): ApiMsg => (Model, Cmd[IO, Msg]) =
    case ApiMsg.Refresh =>
      log("ApiUpdate > update > refresh")
      log((model, OnApiMsg.getRandomGif("random")))
    case ApiMsg.GetNewGif(r) =>
      log("모델에서, 새로운 url로 업데이트 하면된다")
      log(r)
      log((model, Cmd.None))
    case ApiMsg.GetError(_) =>
      log("리프레시 > 에러 나옴")
      log((model, Cmd.None))
