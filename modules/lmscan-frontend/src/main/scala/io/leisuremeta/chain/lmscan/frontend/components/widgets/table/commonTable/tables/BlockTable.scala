// package io.leisuremeta.chain.lmscan.frontend
// import tyrian.Html.*
// import tyrian.*

// object BlockTable:
//   def view(model: Model): Html[Msg] =
//     div(`class` := "table-container")(
//       Title.block(model),
//       Table.block(model), {
//         model.curPage match
//           case PageName.Blocks(_) => Search.search_block(model)
//           case _                  => div()
//       },
//     )
