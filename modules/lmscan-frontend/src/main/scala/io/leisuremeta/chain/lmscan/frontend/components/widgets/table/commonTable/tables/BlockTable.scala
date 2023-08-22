package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockTable:
  def mainView(model: Model) =
    div(`class` := "table-container  position-relative y-center  ")(
      div(`class` := "m-10px w-[100%] ")(
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
        Table.block(model.mainPage.blcList),
        model.mainPage.blcList.totalCount match 
          case None => LoaderView.view
          case Some(_) => div(),
      ),
    )

  def view(model: Model) =
    div(`class` := "table-container  position-relative y-center  ")(
      div(`class` := "m-10px w-[100%] ")(
        div()(
          Title.block(model),
          Table.block(model), {
            find_current_PageCase(model) match
              case DashBoard(_, _, _, _) => div(`class` := "hidden")()
              case _                     => Search.search_block(model)
          },
        ),
        current_ViewCase(model).blockInfo(0) != new BlockInfo match
          case false => LoaderView.view(model)
          case _     => div(`class` := "")(),
      ),
    )
