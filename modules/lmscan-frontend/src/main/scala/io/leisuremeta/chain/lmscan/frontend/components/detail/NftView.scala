package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NftView:
  def view(model: Model): Html[Msg] =
    div(`class` := "pb-32px p-16px color-white")(
      NftDetailTable.view(model),
      TransactionTable.view(model),
    )
