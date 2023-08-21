package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object DefaultLayout:
  def view(model: Model, contents: Html[Msg]): Html[Msg] =
    div(`class` := "main")(
      NavBar.view(model),
      div(id := "page", `class` := "")(
        div(`class` := "x")(
          SearchView.view(model),
          model.commandMode match
            case CommandCaseMode.Development => Toggle.view(model)
            case CommandCaseMode.Production  => div(),
        ), 
        contents,
      ),
      PopupView.view(model),
    )
