package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NftView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px p-16px")(
      div(`class` := "x gap-32px")(
        div(`class` := "nft-image")("그림1"),
        CommonDetailTable.view(model),
      ),
      CommonTableView.view(model),
    )
