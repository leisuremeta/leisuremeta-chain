package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object NoPageView:
  def view(model: Model): Html[Msg] =
    div(`class` := "x-center")(
      div()(
        div(`class` := "x-center")(
          span(`class` := "xy-center font-20px h-64px")("No results Found."),
        ),
        div(`class` := "cell type-button")(
          span(`class` := "font-20px")("Back to Previous Page"),
        ),
      ),
    )
