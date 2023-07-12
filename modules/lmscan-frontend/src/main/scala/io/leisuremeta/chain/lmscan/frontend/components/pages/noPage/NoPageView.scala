package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import Log.log

object NoPageView:
  def view(model: Model): Html[Msg] =
    div(`class` := "x-center")(
      div()(
        div(`class` := "x-center")(
          span(`class` := "xy-center font-20px h-64px color-white")(
            "No results Found.",
          ),
        ),
        div(`class` := "cell type-button")(
          span(
            `class` := "font-20px",
            onClick(
              PageMsg.BackObserver,
            ),
          )(
            "Back to Previous Page",
          ),
        ),
      ),
    )
