package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case object MainPage extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => (
      model,
      Cmd.Batch(
        Cmd.Emit(UpdateSummary),
        DataProcess.getData(BlockModel(page = 1)),
        DataProcess.getData(TxModel(page = 1)),
        Nav.pushUrl("/dashboard"),
      )
    )

  def view(m: Model): Html[Msg] =
    m match
      case model: BaseModel =>
        DefaultLayout.view(
          model,
          div(`class` := "color-white")(
            BoardView.view(model),
            Table.mainView(model),
          )
        )

  def url = "/"
