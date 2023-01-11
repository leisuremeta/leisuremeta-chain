package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object DashboardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "")(
      ul(
        li(`class` := "")(
          div(id := "LMPrice", `class` := "resultBox")(
            span("DashboardView1"),
            strong(" 0.294"),
            span(" USDT"),
          ),
        ),
        li(`class` := "")(
          div(id := "blockHeight", `class` := "resultBox")(
            "Block Height 611,455",
          ),
        ),
      ),
    )
