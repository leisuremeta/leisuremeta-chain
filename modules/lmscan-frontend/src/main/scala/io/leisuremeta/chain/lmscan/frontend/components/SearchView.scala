package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object SearchView:
  def view(model: Model): Html[Msg] =
    div(`class` := "search-area")(
      div(`class` := "search-container xy-center")(
        input(
          onInput(s => GlobalInput(s)),
          onKeyUp(e =>
            e.key match
              case "Enter" => GlobalSearch
              case _       => NoneMsg
          ),
          value   := s"${model.global.searchValue}",
          `class` := "search-text xy-center",
          `placeholder` := (
            "block hash, tx hash, account ... "
          ),
        ),
        div(
          onClick(GlobalSearch),
          `class` := "search-icon xy-center material-symbols-outlined",
        )(
          "search",
        ),
      ),
    )
