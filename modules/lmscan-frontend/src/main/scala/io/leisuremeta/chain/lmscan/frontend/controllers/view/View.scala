package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import tyrian.Html.*

object View:

  def view(model: Model): Html[Msg] =
    div(
      div(`class` := "main")(
        // NavView.view(model),
        PageView.view(model),
      ),
    )
