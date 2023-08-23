package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
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
              onClick(PageMsg.PreUpdate(Blocks())),
            )("More"),
          ),
        ),
        Table.block(model.mainPage.bList),
        model.mainPage.bList.totalCount match 
          case None => LoaderView.view
          case Some(_) => div(),
      ),
    )

  def view(model: Model) =
    div(`class` := "table-container position-relative y-center")(
      div(`class` := "m-10px w-[100%]")(
        div(
          Table.block(model.blcPage.list),
          Search.view(model.blcPage),
        ),
        model.blcPage.list.totalCount match
          case None => LoaderView.view
          case Some(_) => div()
      ),
    )
