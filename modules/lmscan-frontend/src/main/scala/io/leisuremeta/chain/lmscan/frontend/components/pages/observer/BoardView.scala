// package io.leisuremeta.chain.lmscan.frontend

// import tyrian.Html.*
// import tyrian.*

// object BoardView:
//   val view = (model: Model) =>
//     div(`class` := "table-area")(
//       div(`class` := "font-40px pt-16px font-block-detail color-white")(
//         "Observers",
//       ),
//       div(`class` := "table-list x", id := "oop-table-blocks")(
//         div(`class` := "table-container")(
//           div(`class` := "table w-[100%]")(
//             Head.observer :: Body.observer(model),
//           ),
//         ),
//       ),
//     )
