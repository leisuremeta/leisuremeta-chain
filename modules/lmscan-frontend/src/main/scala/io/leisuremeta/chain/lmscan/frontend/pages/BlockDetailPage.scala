package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class BlockDetailPage(hash: String) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ =>
    (
      model,
      Cmd.Emit(UpdateBlcDetailPage(hash)),
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )(
          "Block Details",
        ),
        BlockDetailTable.view(model.blcDetail),
        Table.view(model.blcDetail)
      ),
    )
