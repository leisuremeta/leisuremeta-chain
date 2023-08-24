package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class BlockDetailPage(name: String, hash: String, bn: Long) extends Page:
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
        div(`class` := "x")(
          div(`class` := "y-start gap-10px w-[100%] ")(
            BlockDetailTable.view(model.blcDetail),
            Table.view(model.blcDetail)
          ),
        )
      ),
    )

object BlockDetailPage:
  val name                              = "block"
  def apply: BlockDetailPage               = BlockDetailPage(name, "", 0)
  def apply(hash: String): BlockDetailPage = BlockDetailPage(name, hash, 0)
  def apply(n: Long): BlockDetailPage = BlockDetailPage(name, "", n)
