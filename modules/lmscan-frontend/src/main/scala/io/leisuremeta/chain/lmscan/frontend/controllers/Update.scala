package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case navMsg: NavMsg     => NavUpdate.update(model)(navMsg)
    case inputMsg: InputMsg => InputUpdate.update(model)(inputMsg)
