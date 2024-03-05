package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object TxDetailPage:
  def update(model: TxDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.txDetail))
    case RefreshData => (model, Cmd.None)
    case UpdateDetailPage(d: TxDetail) => (model, DataProcess.getData(d))
    case UpdateModel(v: TxDetail) => (TxDetailModel(txDetail = v), Nav.pushUrl(model.url))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: TxDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(cls := "page-title")("Transaction details") :: 
      commonView(model.txDetail) ::
      TxDetailTableCommon.view(model.txDetail),
    )

  def commonView(data: TxDetail) =
    div(cls := "detail table-container")(
      div(cls := "row")(
        span("Transaction Hash"),
        span(data.hash.getOrElse("-")),
      ),
      div(cls := "row")(
        span("Created At"),
        ParseHtml.fromDate(data.createdAt),
      ),
      div(cls := "row")(
        span("Signer"),
        ParseHtml.fromAccHash(data.signer),
      ),
      div(cls := "row")(
        span("Type"),
        span(data.txType.getOrElse("")),
      ),
      div(cls := "row")(
        span("SubType"),
        span(data.subType.getOrElse("")),
      ),
    )

final case class TxDetailModel(
    global: GlobalModel = GlobalModel(),
    txDetail: TxDetail = TxDetail(),
) extends Model:
    def view: Html[Msg] = TxDetailPage.view(this)
    def url = s"/tx/${txDetail.hash.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = TxDetailPage.update(this)
