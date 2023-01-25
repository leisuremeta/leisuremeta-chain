package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import Log.log

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case navMsg: NavMsg       => NavUpdate.update(model)(navMsg)
    case inputMsg: InputMsg   => SearchUpdate.update(model)(inputMsg)
    case toggleMsg: ToggleMsg => ToggleUpdate.update(model)(toggleMsg)
    case apiMsg: ApiMsg       => ApiUpdate.update(model)(apiMsg)
