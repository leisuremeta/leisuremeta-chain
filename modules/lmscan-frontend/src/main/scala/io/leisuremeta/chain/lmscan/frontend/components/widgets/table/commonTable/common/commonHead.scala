package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*

object Head:
  val block = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Block"),
      Cell.Head("Age"),
      Cell.Head("Block hash"),
      Cell.Head("Tx count"),
    ),
  )

  val observer = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("#"),
      Cell.Head("page:name"),
      Cell.Head("page:url"),
      Cell.Head("page:pubCase0:page"),
      Cell.Head("page:pubCase0:pub_m1"),
      Cell.Head("page:pubCase0:pub_m2"),
    ),
  )

//   val nft = div(`class` := "row table-head")(
//     gen.cell(
//       Cell.Head("Tx Hash"),
//       Cell.Head("Timestamp"),
//       Cell.Head("Action"),
//       Cell.Head("From"),
//       Cell.Head("To"),
//     ),
//   )

  val tx = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Tx Hash"),
      Cell.Head("Block"),
      Cell.Head("Age"),
      Cell.Head("Signer"),
      // Cell.Head("Type"),
      // Cell.Head("Token Type"),
      Cell.Head("Value"),
    ),
  )

//   val tx_dashBoard = div(`class` := "row table-head")(
//     gen.cell(
//       Cell.Head("Tx Hash"),
//       Cell.Head("Age"),
//       Cell.Head("Signer"),
//     ),
//   )
