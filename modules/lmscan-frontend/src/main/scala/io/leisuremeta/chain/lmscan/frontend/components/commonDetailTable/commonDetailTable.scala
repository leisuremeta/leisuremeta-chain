package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
    //   case NftDetail(_, _, _, _) =>
    //     NftDetailTable.view(model)

    //   case _ =>
    //     div(`class` := "row")(
    //       div(`class` := "cell type-detail-head")("Transcation count"),
    //       div(`class` := "cell type-detail-body")("1234"),
    //     )

object CommonDetailTable:
  def view(model: Model): Html[Msg] =
    div()
