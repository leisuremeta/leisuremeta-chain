package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

// object AccountView:
//   def view(model: Model): Html[Msg] =
//     div(`class` := "pb-32px")(
//       div(`class` := "font-40px pt-16px font-block-detail pb-16px color-white")(
//         "Account",
//       ),
//       div(`class` := "x")(CommonDetailTable.view(model)),
//       div(`class` := "font-40px pt-32px font-block-detail pb-16px color-white")(
//         "Transaction History",
//       ),
//       CommonTableView.view(model),
//     )

object NftView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px p-16px color-white")(
      CommonDetailTable.view(model),
      CommonTableView.view(model),
    )
