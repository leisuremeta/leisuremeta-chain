package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Builder.getNumber

object Head:
  val view = div(`class` := "row table-head")(
    div(`class` := "cell")(span("#")),
    div(`class` := "cell")(span("Page")),
    div(`class` := "cell")(span("url")),
    div(`class` := "cell")(span("page")),
  )

object Body:
  def view = (model: Model) =>
    model.observers.map(observer =>
      div(
        `class` := s"row table-body ${observer.number == getNumber(
            model.observers,
            model.observers.length,
          )}_observer ${model.observerNumber == observer.number}_observer_click",
        onClick(PageMsg.UpdateObserver(observer.number)),
      )(
        div(
          `class` := s"cell type-3 ",
        )(
          span()(observer.number.toString()),
        ),
        div(`class` := "cell type-3")(
          span()(observer.pageCase.name),
        ),
        div(`class` := "cell")(span(observer.pageCase.url)),
        div(`class` := "cell")(
          span()(observer.pageCase.url),
        ),
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
