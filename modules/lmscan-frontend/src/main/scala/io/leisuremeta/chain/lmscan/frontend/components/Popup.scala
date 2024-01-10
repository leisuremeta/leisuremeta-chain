package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object PopupView:
  def view(model: Model): Html[Msg] =
    div(id := "popup1", `class` := s"overlay ${model.global.popup}")(
      div(`class` := s"popup ${model.global.popup}")(
        h2("INFO"),
        br,
        div(
          `class` := "close",
          onClick(PopupMsg(false)),
        )("×"),
        div(`class` := "content")(
          "sorry, page limit is 50,000",
        ),
      ),
    )
