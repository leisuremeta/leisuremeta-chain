package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object ErrorPage:
  def update(model: ErrorModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, Cmd.None)
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: ErrorModel): Html[Msg] =
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
              ToPage(BaseModel()),
            ),
          )(
            "Back to Previous Page",
          ),
        ),
      ),
    )

final case class ErrorModel(
    global: GlobalModel = GlobalModel(),
    error: String
) extends Model:
    def view: Html[Msg] = ErrorPage.view(this)
    def url = "/error"
    def update: Msg => (Model, Cmd[IO, Msg]) = ErrorPage.update(this)
