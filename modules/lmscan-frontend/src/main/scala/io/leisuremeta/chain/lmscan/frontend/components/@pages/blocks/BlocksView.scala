package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BlocksView:
  def view(model: Model): Html[Msg] =
    div(`class` := "")(
      ul(
        li(`class` := "")(
          div(id := "LMPrice", `class` := "resultBox")(
            span("BlocksView1"),
            strong(" 0.294"),
            span(" USDT"),
          ),
        ),
        li(`class` := "")(
          div(id := "blockHeight", `class` := "resultBox")(
            "안녕",
          ),
        ),
      ),
    )
