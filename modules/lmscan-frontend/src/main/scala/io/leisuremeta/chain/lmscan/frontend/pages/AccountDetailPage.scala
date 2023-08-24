package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class AccountDetailPage(name: String, hash: String) extends Page:
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
        div(`class` := "x")(AccountDetailTable.view(model)),
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )("Transaction History"),
        div(`class` := "y-start gap-10px w-[100%] ")(
          TransactionTable.view(model.accDetail)
        ),
      ),
    )

object AccountDetailPage:
  val name                              = "account"
  def apply: AccountDetailPage               = AccountDetailPage(name, "")
  def apply(hash: String): AccountDetailPage = AccountDetailPage(name, hash)
