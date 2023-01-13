package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import Log.log

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case navMsg: NavMsg     => log(NavUpdate.update(model)(navMsg))
    case inputMsg: InputMsg => log(InputUpdate.update(model)(inputMsg))
