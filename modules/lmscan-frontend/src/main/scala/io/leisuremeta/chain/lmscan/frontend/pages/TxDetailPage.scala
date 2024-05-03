package io.leisuremeta.chain
package lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._
import io.circe.*
import io.circe.parser.*
import io.circe.generic.auto.*
import api.model._
import api.model.Transaction.AccountTx
import api.model.Transaction.TokenTx
import api.model.Transaction.GroupTx
import api.model.Transaction.RewardTx
import api.model.Transaction.AgendaTx

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
      commonView(model.txDetail, model.getInfo) ::
      TxDetailTableCommon.view(model.getTxr),
    )

  def commonView(data: TxDetail, info: (String, String, String)) =
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
        ParseHtml.fromAccHash(Some(info._1)),
      ),
      div(cls := "row")(
        span("Type"),
        span(info._2),
      ),
      div(cls := "row")(
        span("SubType"),
        span(info._3),
      ),
    )

final case class TxDetailModel(
    global: GlobalModel = GlobalModel(),
    txDetail: TxDetail = TxDetail(),
) extends Model:
    def view: Html[Msg] = TxDetailPage.view(this)
    def url = s"/tx/${txDetail.hash.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = TxDetailPage.update(this)
    def getTxr: Option[TransactionWithResult] =
      val res = for
        json <- txDetail.json
        tx <- decode[TransactionWithResult](json).toOption
      yield tx
      res
    def getInfo: (String, String, String) =
      extension (a: Transaction)
        def splitTx = a.toString.split("\\(").head
      this.getTxr match
        case Some(x) =>
          val acc = x.signedTx.sig.account.toString
          val (tt, st) = x.signedTx.value match
            case tx: Transaction.TokenTx => ("TokenTx", tx.splitTx)
            case tx: Transaction.AccountTx => ("TokenTx", tx.splitTx)
            case tx: Transaction.GroupTx => ("TokenTx", tx.splitTx)
            case tx: Transaction.RewardTx => ("TokenTx", tx.splitTx)
            case tx: Transaction.AgendaTx => ("TokenTx", tx.splitTx)
          (acc, tt, st)
        case None => ("", "", "")
