package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class NftTokenPage(id: String, page: Int = 1) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => 
    (model, Cmd.Emit(UpdateNftTokenPage(id, page)))

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Nfts Token",
        ),
        Table.view(model.nftTokenPage),
      ),
    )
