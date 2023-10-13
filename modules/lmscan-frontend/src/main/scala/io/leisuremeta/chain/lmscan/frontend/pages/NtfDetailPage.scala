package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class NftDetailPage(hash: String) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ =>
    (
      model,
      Cmd.Emit(UpdateNftDetailPage(hash)),
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px pt-16px color-white")(
        NftDetailTable.view(model.nftDetail),
        Table.view(model.nftDetail)
      ),
    )

  def url = s"/nft/$hash"
