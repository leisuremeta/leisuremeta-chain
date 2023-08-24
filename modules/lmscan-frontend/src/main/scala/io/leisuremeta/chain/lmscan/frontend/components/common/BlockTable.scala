package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockTable:
  def mainView(model: Model) =
    div(`class` := "table-container position-relative y-center")(
      div(`class` := "m-10px w-[100%]")(
        div(
          `class` := s"table-title",
        )(
          div(
            `class` := s"type-1",
          )(span()("Latest Blocks")),
          div(
            `class` := s"type-2",
          )(
            span(
              onClick(
                RouterMsg.NavigateTo(BlockPage)
              ),
            )("More"),
          ),
        ),
        model.blcPage.list match 
          case None => LoaderView.view
          case Some(v) => Table.block(v),
      ),
    )

  def view(model: Model) =
    div(`class` := "table-container position-relative y-center")(
      div(`class` := "m-10px w-[100%]")(
        div(
          Table.block(model.blcPage.list.get),
          Pagination.view(model.blcPage),
        ),
        model.blcPage.list match
          case None => LoaderView.view
          case Some(_) => div()
      ),
    )
