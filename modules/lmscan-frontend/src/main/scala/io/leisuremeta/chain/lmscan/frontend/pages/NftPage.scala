package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object NftPage:
  def update(model: NftModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, Cmd.None)
    case UpdateListModel(v: PageResponse[NftInfoModel]) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (NftModel(page = model.searchPage), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: NftModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("NFTs"),
        Table.view(model),
      ),
    )

final case class NftModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[NftInfoModel]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def view: Html[Msg] = NftPage.view(this)
    def url = s"/nfts/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = NftPage.update(this)
