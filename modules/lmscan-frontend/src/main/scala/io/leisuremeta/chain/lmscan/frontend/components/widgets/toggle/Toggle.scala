package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.current_ViewCase
import dotty.tools.dotc.reporting.trace.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.get_PageResponseViewCase

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

  def detail_button(model: Model): Html[Msg] =
    div(
      div(
        `class` := s"type-2 pt-32px",
      )(
        get_PageResponseViewCase(model).txDetail.txType
          .getOrElse("") == "Agenda" match

          case true =>
            span(
              `class` := s"${model.detail_button}",
              onClick(DetailButtonMsg.OnClick(model.detail_button)),
            )("details")
            div()
          case false => div(),
      ),
    )
  def detail_view(model: Model): Html[Msg] =
    get_PageResponseViewCase(model).txDetail.txType
      .getOrElse("") == "Agenda" match
      case true =>
        model.detail_button match
          case true  => JsonView.view(model)
          case false => div()
      case false =>
        div()
