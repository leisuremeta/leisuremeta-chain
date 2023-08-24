package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case object MainPage extends Page:
  val name = "Dashboard"
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ => (
      model,
      Cmd.Batch(
        Cmd.Emit(UpdateSummary),
        DataProcess.getData(BlockModel(page = 1)),
        DataProcess.getData(TxModel(page = 1)),
        Nav.pushUrl("/dashboard"),
      )
    )

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      render(model)
    )
  
  def render(model: Model): Html[Msg] =
    div(`class` := "color-white")(
      BoardView.view(model),
      div(`class` := "table-area")(
        div(id := "oop-table-blocks", `class` := "table-list x")(
          BlockTable.mainView(model),
          TransactionTable.mainView(model),
        ),
      )
    )
