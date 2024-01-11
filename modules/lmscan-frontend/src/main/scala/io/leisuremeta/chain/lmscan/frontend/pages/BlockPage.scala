package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case class  BlockPage(page: Int) extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => 
    (model, Cmd.Emit(UpdateBlockPage(page)))

  def view(m: Model): Html[Msg] =
    m match
      case model: BaseModel =>
        DefaultLayout.view(
          model,
          div(`class` := "table-area")(
            div(`class` := "font-40px pt-16px font-block-detail color-white")(
              "Blocks",
            ),
            Table.view(model.blcPage),
          ),
        )

  def url = s"/blcs/$page"
