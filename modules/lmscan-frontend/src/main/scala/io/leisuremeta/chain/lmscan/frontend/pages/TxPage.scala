package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object TxPage extends Page:
  val name = "Transactions"
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => (
      model,
      Cmd.Emit(PageMsg.UpdateTxs)
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Transactions",
        ),
        div(id := "oop-table-blocks", `class` := "table-list x")(
          TransactionTable.view(model),
        ),
      ),
    )
