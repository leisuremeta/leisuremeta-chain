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

  val accs = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Address"),
      Cell.Head("Balance"),
      Cell.Head("Value"),
      Cell.Head("Last Seen"),
    ),
  )

  val nfts = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head(""),
      Cell.Head("Season"),
      Cell.Head("NFT"),
      Cell.Head("Total Supply"),
      Cell.Head("Sale Started"),
      Cell.Head("Sale Ended"),
    ),
  )

  val nftToken = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("NFT"),
      Cell.Head("Collection"),
      Cell.Head("Token ID"),
      Cell.Head("Creator"),
      Cell.Head("Rarity"),
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

  val tx = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Tx Hash"),
      Cell.Head("Block"),
      Cell.Head("Age"),
      Cell.Head("Signer"),
      Cell.Head("Value"),
    ),
  )

  val tx2 = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Tx Hash"),
      Cell.Head("Block"),
      Cell.Head("Age"),
      Cell.Head("Signer"),
      Cell.Head("Subtype"),
      Cell.Head("Value"),
    ),
  )

  val tx_dashBoard = div(`class` := "row table-head")(
    gen.cell(
      Cell.Head("Tx Hash"),
      Cell.Head("Age"),
      Cell.Head("Signer"),
    ),
  )
