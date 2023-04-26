package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Toggle:
  def view(model: Model): Html[Msg] =
    div(onClick(ToggleMsg.OnClick(model.toggle)))(
      div(`class` := "toggle-area xy-center")(
        input(
          `class` := s"${model.toggle}",
          `type` := "checkbox", // checked attribute 대신에 class 로 대신하는게 더 자유도가 높다.
          id := "toggle",
        ),
        label(_for := "toggle")(),
      ),
    )
