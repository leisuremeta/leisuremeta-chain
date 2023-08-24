package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model.TxDetail

case class TxDetailPage(name: String, hash: String) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ =>
    (model, Cmd.Emit(UpdateTxDetailPage(hash)))

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )(
          "Transaction details",
        ),
        div(`class` := "x")(
          div(`class` := "y-start gap-10px w-[100%] ")(
            TxDetailTableMain.view(model.txDetail) :: TxDetailTableCommon.view(
              model.txDetail,
            ),
          ),
        ),
      ),
    )

object TxDetailPage:
  val name                              = "tx"
  def apply: TxDetailPage               = TxDetailPage(name, "")
  def apply(hash: String): TxDetailPage = TxDetailPage(name, hash)
  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )(
          "Transaction details",
        ),
        div(`class` := "x")(
          div(`class` := "y-start gap-10px w-[100%] ")(
            TxDetailTableMain.view(model.txDetail) :: TxDetailTableCommon.view(
              model.txDetail,
            ),
          ),
        ),
      ),
    )
