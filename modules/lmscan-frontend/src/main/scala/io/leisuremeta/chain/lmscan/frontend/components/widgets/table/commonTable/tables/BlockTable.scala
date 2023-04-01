package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.get_ViewCase
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container x  y-center  position-relative")(
      div(`class` := "m-10px w-[100%] ")(
        div(`class` := "  ")(
          Title.block(model),
          Table.block(model),
        ),
        get_ViewCase(model).blockInfo(0) != new BlockInfo match
          case true => LoaderView.view(model)
          case _    => div(`class` := "")(),
      ),
    )
