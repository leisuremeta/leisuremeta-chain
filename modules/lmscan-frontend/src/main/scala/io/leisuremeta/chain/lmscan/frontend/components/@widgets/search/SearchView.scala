package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object SearchView:
  def view(model: Model): Html[Msg] =
    div(
      div(`class` := "search-area center")(
        div(`class` := "search-container center")(
          input(
            `class` := "search-text center",
            `placeholder` := (
              "block number, block hash, account, tx hash",
            ),
          ),
          div(`class` := "search-icon center material-symbols-outlined")(
            "search",
          ),
        ),
      ),
    )
