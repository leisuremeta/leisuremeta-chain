package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object BlockDetailPage:
  def update(model: BlcDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, Cmd.None)
    case UpdateDetailPage(d: BlockDetail) => (model, DataProcess.getData(model.copy(blcDetail = d)))
    case UpdateModel(v: BlockDetail) => (model.set(v), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (model.copy(page = model.searchPage, pageToggle = false), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: BlcDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Block Details"),
        model.data match
          case None => LoaderView.view
          case Some(_) => BlockDetailTable.view(model.blcDetail)
        ,
        div(cls := "page-title")("Transaction List"),
        model.data match
          case None => LoaderView.view
          case Some(_) => Table.view(model)
      ),
    )

final case class BlcDetailModel(
    global: GlobalModel = GlobalModel(),
    blcDetail: BlockDetail = BlockDetail(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[TxInfo]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def set(v: BlockDetail) =
      val data = if v.payload.length == 0 then Some(PageResponse()) else Some(PageResponse(v.totalCount, v.totalPages, v.payload))
      this.copy(
        blcDetail = v,
        data = data,
      )
    def view: Html[Msg] = BlockDetailPage.view(this)
    def url = s"/blc/${blcDetail.hash.get}?p=${page}"
    def update: Msg => (Model, Cmd[IO, Msg]) = BlockDetailPage.update(this)
