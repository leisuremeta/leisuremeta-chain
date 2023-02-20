package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object NftTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Title.nft(model),
      Table.nft(model),
    )
