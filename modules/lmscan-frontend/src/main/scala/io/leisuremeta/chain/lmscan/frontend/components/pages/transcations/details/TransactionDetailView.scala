package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.current_ViewCase
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.get_PageResponseViewCase

object TransactionDetailView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px")(
      div(`class` := "font-40px pt-16px font-block-detail pb-16px color-white")(
        "Transaction details",
      ),
      div(`class` := "x")(CommonDetailTable.view(model)),
      div(Toggle.detail_button(model)),
      // agenda 일때만 보여주도록 수정
      {

        get_PageResponseViewCase(model).txDetail.txType
          .getOrElse("") == "Agenda" match // true == is agenda?

          case true =>
            model.detail_button match
              case true  => JsonView.view(model)
              case false => div()
          case false =>
            div()
      },
    )
