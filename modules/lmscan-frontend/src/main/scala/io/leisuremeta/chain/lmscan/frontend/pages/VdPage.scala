package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object VdPage:
  def update(model: VdModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, DataProcess.getData(model))
    case UpdateModel(v: NodeValidator.ValidatorList) => (model.copy(payload = v.payload.toList), Nav.pushUrl(model.url))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: VdModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Validators"),
        Table.view(model),
      ),
    )

final case class VdModel(
    global: GlobalModel = GlobalModel(),
    payload: List[NodeValidator.Validator] = List()
) extends Model:
    def view: Html[Msg] = VdPage.view(this)
    def url = s"/vds"
    def update: Msg => (Model, Cmd[IO, Msg]) = VdPage.update(this)
    