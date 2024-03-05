package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object AccountPage:
  def update(model: AccModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case UpdateListModel(v: PageResponse[AccountInfo]) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (AccModel(page = model.searchPage), Cmd.emit(Init))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: AccModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Accounts",
        ),
        Table.view(model),
      ),
    )

final case class AccModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[AccountInfo]] = None,
) extends PageModel:
    def view: Html[Msg] = AccountPage.view(this)
    def url = s"/accs/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = AccountPage.update(this)
