package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Builder.getPage

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Title.block(model),
      Table.block(model), {
        getPage(model.observers, model.observerNumber) match
          case PageCase.Blocks(_, _, _) => Search.search_block(model)
          case _                        => div()
      },
    )
