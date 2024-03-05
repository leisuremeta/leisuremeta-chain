package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object NftTokenPage:
  def update(model: NftTokenModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, Cmd.None)
    case UpdateListModel(v: PageResponse[NftSeasonModel]) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (NftTokenModel(page = model.searchPage, id = model.id), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: NftTokenModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("NFTs Token"),
        Table.view(model),
      ),
    )

final case class NftTokenModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    id: String,
    data: Option[PageResponse[NftSeasonModel]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def view: Html[Msg] = NftTokenPage.view(this)
    def url = s"/nft/$id/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = NftTokenPage.update(this)
