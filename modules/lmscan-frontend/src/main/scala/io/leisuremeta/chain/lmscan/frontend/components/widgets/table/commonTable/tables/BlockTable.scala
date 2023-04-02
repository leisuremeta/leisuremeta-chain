package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container  position-relative y-center  ")(
      div(`class` := "m-10px w-[100%] ")(
        div(`class` := "  ")(
          Title.block(model),
          Table.block(model), {
            find_current_PageCase(model) match
              case PageCase.DashBoard(_, _, _, _) => div(`class` := "hidden")()
              case _                              => Search.search_block(model)
          },
        ),
        current_ViewCase(model).blockInfo(0) != new BlockInfo match
          case false => LoaderView.view(model)
          case _     => div(`class` := "")(),
      ),
    )
