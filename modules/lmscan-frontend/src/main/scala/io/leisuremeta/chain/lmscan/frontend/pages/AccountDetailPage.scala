package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object AccountDetailPage:
  def update(model: AccDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.accDetail))
    case UpdateDetailPage(d: AccountDetail) => (model, DataProcess.getData(d))
    case UpdateModel(v: AccountDetail) => (AccDetailModel(accDetail = v), Nav.pushUrl(model.url))
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: AccDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )("Account"),
        AccountDetailTable.view(model.accDetail),
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )("Transaction History"),
        Table.view(model.accDetail)
      ),
    )

final case class AccDetailModel(
    global: GlobalModel = GlobalModel(),
    accDetail: AccountDetail = AccountDetail(),
) extends Model:
    def view: Html[Msg] = AccountDetailPage.view(this)
    def url = s"/acc/${accDetail.address.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = AccountDetailPage.update(this)
