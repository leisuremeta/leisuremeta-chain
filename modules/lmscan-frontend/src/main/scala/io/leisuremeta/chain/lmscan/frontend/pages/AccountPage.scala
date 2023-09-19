package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class  AccountPage(page: Int) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => 
    (model, Cmd.Emit(UpdateAccPage(page)))

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Accounts",
        ),
        Table.view(model.accPage),
      ),
    )

  def url = s"/accounts/$page"
