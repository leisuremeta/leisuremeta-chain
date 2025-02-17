package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object VdDetailPage:
  def update(model: VdDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, DataProcess.getData(model))
    case UpdateModel(v: NodeValidator.ValidatorDetail) => 
      (model.copy(payload = v), Nav.pushUrl(model.url))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: VdDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Validator Detail"),
        infoView(model.validator),
        div(cls := "page-title")("Proposed Blocks"),
        Table.view(model),
      ),
    )
  
  def infoView(data: NodeValidator.Validator) =
      div(cls := "detail table-container")(
        div(cls := "row")(
          span("address"),
          span(data.address.getOrElse("")),
        ),
        div(cls := "row")(
          span("Total Proposed Blocks"),
          span(f"${data.cnt.getOrElse(0L)}%,d"),
        ),
        div(cls := "row")(
          span("Voting Power"),
          span(data.power.getOrElse(0.0).toString + "%"),
        ),
      )

final case class VdDetailModel(
    global: GlobalModel = GlobalModel(),
    address: String,
    page: Int = 1,
    searchPage: Int = 1,
    pageToggle: Boolean = false,
    payload: NodeValidator.ValidatorDetail = NodeValidator.ValidatorDetail()
) extends PageModel:
    def view: Html[Msg] = VdDetailPage.view(this)
    def url = s"/vds/$address?p=$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = VdDetailPage.update(this)
    def validator = payload.validator
    def blcs = payload.page
    val data = Some(payload.page)
    