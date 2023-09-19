package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class AccountDetailPage(hash: String) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ =>
    (
      model,
      Cmd.Emit(UpdateAccDetailPage(hash)),
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )("Account"),
        AccountDetailTable.view(model),
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )("Transaction History"),
        Table.view(model.accDetail)
      ),
    )

  def url = s"/account/$hash"
