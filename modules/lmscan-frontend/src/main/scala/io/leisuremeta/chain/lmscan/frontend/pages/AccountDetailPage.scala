package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object AccountDetailPage:
  def update(model: AccDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.accDetail))
    case RefreshData => (model, Cmd.None)
    case UpdateDetailPage(d: AccountDetail) => (model, DataProcess.getData(d))
    case UpdateModel(v: AccountDetail) => (AccDetailModel(accDetail = v), Nav.pushUrl(model.url))
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
) extends Model:
    def view: Html[Msg] = AccountDetailPage.view(this)
    def url = s"/acc/${accDetail.address.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = AccountDetailPage.update(this)
