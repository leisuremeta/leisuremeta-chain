package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.get_ViewCase
import io.leisuremeta.chain.lmscan.common.model.BlockInfo

object BlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Title.block(model),
      div(`class` := "loader-container table w-[100%] ")({
        get_ViewCase(model).blockInfo(0) != new BlockInfo match
          case true => {
            div()(
              Table.block(model), {
                find_PageCase(model.pointer)(model.appStates) match
                  case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
                  case _                           => div()
              },
            )
          }
          case _ =>
            // TODO : size
            div(`class` := "xy-center h-[100%]")(div(`class` := "loader")())

          // div()(
          //   Table.block(model), {
          //     find_PageCase(model.pointer)(model.appStates) match
          //       case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
          //       case _                           => div()
          //   },
          // )
      }),

      // Table.block(model), {
      //   find_PageCase(model.pointer)(model.appStates) match
      //     case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
      //     case _                           => div()
      // },
    )

  // def view(model: Model): Html[Msg] =
  //   div(`class` := "table-container")(
  //     Title.block(model),
  //     div(`class` := "loader-container w-[100%]")({
  //       get_ViewCase(model).blockInfo(0) != new BlockInfo match
  //         case true => {
  //           div()(
  //             Table.block(model), {
  //               find_PageCase(model.pointer)(model.appStates) match
  //                 case PageCase.Blocks(_, _, _, _) => Search.search_block(model)
  //                 case _                           => div()
  //             },
  //           )
  //         }
  //         case _ => div(`class` := "loader")()
  //     }),
  //   )
