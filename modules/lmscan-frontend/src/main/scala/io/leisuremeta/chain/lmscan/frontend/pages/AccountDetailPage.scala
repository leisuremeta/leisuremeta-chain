package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object AccountDetailPage:
  def update(model: AccDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model))
    case RefreshData => (model, Cmd.None)
    case UpdateDetailPage(d: AccountDetail) => (model, DataProcess.getData(model.copy(accDetail = d)))
    case UpdateModel(v: AccountDetail) => (model.set(v), Nav.pushUrl(model.url))
    case UpdateSearch(v) => (model.copy(searchPage = v), Cmd.None)
    case ListSearch => (model.copy(page = model.searchPage, pageToggle = false), Cmd.emit(Init))
    case TogglePageInput(t) => (model.copy(pageToggle = t), Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: AccDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        div(cls := "page-title")("Account"),
        AccountDetailTable.view(model.accDetail),
        div(cls := "page-title")("Transaction History"),
        Table.view(model)
      ),
    )

final case class AccDetailModel(
    global: GlobalModel = GlobalModel(),
    accDetail: AccountDetail = AccountDetail(),
    page: Int = 1,
    searchPage: Int = 1,
    data: Option[PageResponse[TxInfo]] = None,
    pageToggle: Boolean = false,
) extends PageModel:
    def set(v: AccountDetail) =
      val data = if v.payload.length == 0 then Some(PageResponse()) else Some(PageResponse(v.totalCount, v.totalPages, v.payload))
      this.copy(
        accDetail = v,
        data = data,
      )

    def view: Html[Msg] = AccountDetailPage.view(this)
    def url = s"/acc/${accDetail.address.get}?p=${page}"
    def update: Msg => (Model, Cmd[IO, Msg]) = AccountDetailPage.update(this)
