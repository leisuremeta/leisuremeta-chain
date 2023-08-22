package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
object DetailTables:
  def render(model: Model): Html[Msg] =
    div()
    // find_current_PageCase(model) match
    //   case BlockDetail(_, _, _, _) =>
    //     BlockDetailTable.view(model)

    //   case TxDetail(_, _, _, _) =>
    //     TxDetailTable.view(model)

    //   case NftDetail(_, _, _, _) =>
    //     NftDetailTable.view(model)

    //   case AccountDetail(_, _, _, _) =>
    //     AccountDetailTable.view(model)

    //   case _ =>
    //     div(`class` := "row")(
    //       div(`class` := "cell type-detail-head")("Transcation count"),
    //       div(`class` := "cell type-detail-body")("1234"),
    //     )

object CommonDetailTable:
  def view(model: Model): Html[Msg] =
    DetailTables.render(model)
