package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object BlockPage:
  def update(model: BlcModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, DataProcess.getData(model))
    case UpdateBlcs(v) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (BlcModel(page = model.searchPage), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: BlcModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Blocks"),
        Table.view(model),
      ),
    )

final case class BlcModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[BlockInfo]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def view: Html[Msg] = BlockPage.view(this)
    def url = s"/blcs/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = BlockPage.update(this)
