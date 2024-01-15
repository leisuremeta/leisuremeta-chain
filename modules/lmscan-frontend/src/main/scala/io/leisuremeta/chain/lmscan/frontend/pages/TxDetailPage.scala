package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object TxDetailPage:
  def update(model: TxDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.txDetail))
    case UpdateDetailPage(d: TxDetail) => (model, DataProcess.getData(d))
    case UpdateModel(v: TxDetail) => (TxDetailModel(txDetail = v), Nav.pushUrl(model.url))
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: TxDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )(
          "Transaction details",
        ) :: TxDetailTableMain.view(model.txDetail) :: TxDetailTableCommon.view(
          model.txDetail,
        ),
      ),
    )

final case class TxDetailModel(
    global: GlobalModel = GlobalModel(),
    txDetail: TxDetail = TxDetail(),
) extends Model:
    def view: Html[Msg] = TxDetailPage.view(this)
    def url = s"/tx/${txDetail.hash.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = TxDetailPage.update(this)
