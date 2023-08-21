package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object JsonView:
  def view(model: Model): Html[Msg] = div(`class` := "y-center")(
    {
      find_current_Pub_m1s(model)
        .filter(d => d != "")
        .map(d =>
          div(
            textarea(
              spellcheck := "false",
              `id`       := s"transaction-text-area",
            )(s"${d}"),
          ),
        )
    },
  )
