package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
object DetailTables:
  def render(model: Model): Html[Msg] =
    get_PageCase(model) match
      case PageCase.BlockDetail(_, _, _, _) =>
        BlockDetailTable.view(model)

      case PageCase.TxDetail(_, _, _, _) =>
        TxDetailTable.view(model)

      case PageCase.AccountDetail(_, _, _, _) =>
        AccountDetailTable.view(model)
      //   case PageName.NftDetail(_) =>
      //     NftDetailTable.view(model)

      case _ =>
        div(`class` := "row")(
          div(`class` := "cell type-detail-head")("Transcation count"),
          div(`class` := "cell type-detail-body")("1234"),
        )

object CommonDetailTable:
  def view(model: Model): Html[Msg] =
    DetailTables.render(model)
