package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class NftPage(page: Int) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => 
    (model, Cmd.Emit(UpdateNftPage(page)))

  def view(m: Model): Html[Msg] =
    m match
      case model: BaseModel =>
        DefaultLayout.view(
          model,
          div(`class` := "table-area")(
            div(`class` := "font-40px pt-16px font-block-detail color-white")(
              "NFTs",
            ),
            Table.view(model.nftPage),
          ),
        )

  def url = s"/nfts/$page"
