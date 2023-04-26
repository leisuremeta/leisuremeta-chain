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

object JsonView:
  def view(model: Model): Html[Msg] = div()(
    {
      log(
        find_current_Pub_m1s(model).filter(d => d != ""),
      ) // dash 보드면 3개가 떠야하는데 .. 0~3 까지 순차적으로 뜬다
      log(find_current_Pub_m1s(model).filter(d => d != "").length)
      ""
    },
    // div(find_current_Pub_m1s(model).filter(d => d != "")(0)),
    // div(find_current_Pub_m1s(model).filter(d => d != "")(1)),
  )
