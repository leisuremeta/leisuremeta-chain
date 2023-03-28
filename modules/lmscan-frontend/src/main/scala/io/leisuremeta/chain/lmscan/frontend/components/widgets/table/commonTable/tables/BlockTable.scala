package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.get_ViewCase
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

// = origin
// object BlockTable:
//   def view(model: Model): Html[Msg] =
//     div(`class` := "table-container xy-center position-relative")(
//       Title.block(model),
//       Table.block(model), {
//         find_PageCase(model.pointer)(model.appStates) match
//           case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
//           case _                           => div()
//       },
//       LoaderView.view(model),
//       // {
//       //   get_ViewCase(model).blockInfo(0) != new BlockInfo match
//       //     case true =>
//       //       LoaderView.view(model)

//       //     case _ => div()
//       // },
//     )

object BlockTable:
  def view(model: Model): Html[Msg] =
    // div(`class` := "position-relative table-container")(
    div(`class` := "table-container x position-relative y-center  ")(
      div(`class` := "  m-10px w-block-list h-[100%] ")(
        Title.block(model),
        Table.block(model),
        // LoaderView.view(model),
      ),
      get_ViewCase(model).blockInfo(0) != new BlockInfo match
        case false => LoaderView.view(model)
        case _     => div(),
    )
    // LoaderView.view(model))
  // )
