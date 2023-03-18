package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object Head:
  val view = div(`class` := "row table-head")(
    div(`class` := "cell")(span("pages(page,hash,data)")),
    div(`class` := "cell")(span("url")),
    div(`class` := "cell")(span("as")),
    div(`class` := "cell")(span("Signer")),
    div(`class` := "cell")(span("Value")),
  )

object Body:
  def view = (model: Model) =>
    model.observers.map(observer =>
      div(`class` := "row table-body")(
        div(`class` := "cell type-3")(
          span(
          )({
            observer.pageName.toString()
          }),
        ),
        div(`class` := "cell")(span("1414443")),
        div(`class` := "cell")(
          span(
            Attribute("data-tooltip-text", "2023-03-16 00:27:09"),
          )("8 mins ago"),
        ),
        div(`class` := "cell type-3")(span("9c2418c620...")),
        div(`class` := "cell")(span("-")),
      ),
    )

object BoardView:
  val view = (model: Model) =>
    div(`class` := "table-area")(
      div(`class` := "font-40px pt-16px font-block-detail color-white")(
        "Observers",
      ),
      div(`class` := "table-list x", id := "oop-table-blocks")(
        div(`class` := "table-container")(
          div(`class` := "table w-[100%]")(
            Head.view :: Body.view(model),
          ),
        ),
      ),
    )
