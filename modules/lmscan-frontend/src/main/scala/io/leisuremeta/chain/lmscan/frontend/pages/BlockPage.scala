package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object BlockPage extends Page:
  val name = "Blocks"
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => 
    (model, DataProcess.getData(model.blcPage))

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "table-area")(
        div(`class` := "font-40px pt-16px font-block-detail color-white")(
          "Blocks",
        ),
        div(id := "oop-table-blocks", `class` := "table-list x")(
          BlockTable.view(model),
        ),
      ),
    )
