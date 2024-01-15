package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object TxPage:
  def update(model: TxModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case UpdateTxs(v) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (TxModel(page = model.searchPage), Cmd.emit(Init))
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: TxModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Transactions",
        ),
        Table.view(model),
      ),
    )

final case class TxModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[TxInfo]] = None,
) extends PageModel:
    def view: Html[Msg] = TxPage.view(this)
    def url = s"/txs/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = TxPage.update(this)
