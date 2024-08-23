package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object TxPage:
  def update(model: TxModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => 
      if (model.page == 1) then (model, DataProcess.getData(model))
      else (model, Cmd.None)
    case UpdateTxs(v) => (model.copy(data = Some(v)), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (TxModel(page = model.searchPage), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: TxModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Transactions"),
        Table.view(model),
      ),
    )

final case class TxModel(
    global: GlobalModel = GlobalModel(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[TxInfo]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def view: Html[Msg] = TxPage.view(this)
    def url = s"/txs/$page"
    def update: Msg => (Model, Cmd[IO, Msg]) = TxPage.update(this)
    