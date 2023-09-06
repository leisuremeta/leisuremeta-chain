package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

case object ErrorPage extends Page:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = _ =>
    (model, Cmd.None)

  def view(model: Model): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "x-center", style := "flex-flow:column;align-items:center;")(
        span(`class` := "xy-center font-20px h-64px color-white")(
          "No results Found.",
        ),
        div(`class` := "cell type-button")(
          span(
            `class` := "font-20px",
            onClick(
              RouterMsg.NavigateTo(MainPage),
            ),
          )(
            "Back to Previous Page",
          ),
        ),
      ),
    )
