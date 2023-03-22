package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Title.block(model),
      Table.block(model),
      // {
      //   in_Observer_PageCase(model.observers, model.observerNumber) match
      //     case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
      //     case _                           => div()
      // },
    )
