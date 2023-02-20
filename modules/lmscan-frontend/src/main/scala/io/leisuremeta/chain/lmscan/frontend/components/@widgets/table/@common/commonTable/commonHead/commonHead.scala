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

  val nft = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Tx Hash"),
      Cell.Head("Timestamp"),
      Cell.Head("Action"),
      Cell.Head("From"),
      Cell.Head("To"),
    ),
  )
