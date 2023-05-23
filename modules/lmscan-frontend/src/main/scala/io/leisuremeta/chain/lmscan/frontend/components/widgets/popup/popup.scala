package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object PopupView:
  def view(model: Model): Html[Msg] =
    div(id := "popup1", `class` := "overlay")(
      div(`class` := "popup")(
        h2("Here i am"),
        a(`class` := "close", href := "#")("×"),
        div(`class` := "content")(
          "Thank to pop me out of that button, but now i'm done so you can close this window.",
        ),
      ),
    )
