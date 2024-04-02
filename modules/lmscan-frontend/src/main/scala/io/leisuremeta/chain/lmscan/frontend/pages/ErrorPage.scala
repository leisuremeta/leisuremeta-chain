package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object ErrorPage:
  def update(model: ErrorModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, Cmd.None)
    case RefreshData => (model, Cmd.None)
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: ErrorModel): Html[Msg] =
    DefaultLayout.view(
      model,
      model.error match
        case "timeout" => timeout
        case _ => 
          div(cls := "err-wrap")(
            p("THE PAGE YOU WERE LOOKING FOR DOESN’T EXIST."),
            p("You may have mistyped the information. Please check before searching."),
            div(cls := "cell type-button")(
              span(
                cls := "font-20px",
                onClick(
                  ToPage(BaseModel()),
                ),
              )(
                "Back to Previous Page",
              ),
            ),
          ),
    )

  val timeout = 
    div(cls := "err-wrap")(
      p("TIME OUT! TRY AGAIN LATER.")
    )

final case class ErrorModel(
    global: GlobalModel = GlobalModel(),
    error: String
) extends Model:
    def view: Html[Msg] = ErrorPage.view(this)
    def url = "/error"
    def update: Msg => (Model, Cmd[IO, Msg]) = ErrorPage.update(this)
