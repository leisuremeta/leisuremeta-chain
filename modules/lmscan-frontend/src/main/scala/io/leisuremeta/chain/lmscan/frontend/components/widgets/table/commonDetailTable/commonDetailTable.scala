package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import io.leisuremeta.chain.lmscan.frontend.Builder.getPage
import io.leisuremeta.chain.lmscan.frontend.Log.log

object DetailTables:
  def render(model: Model): Html[Msg] =
    getPage(model) match
      case PageCase.BlockDetail(_, _, _, _) =>
        BlockDetailTable.view(model)
      //   case PageName.TransactionDetail(_) =>
      //     TxDetailTable.view(model)
      //   case PageName.AccountDetail(_) =>
      //     AccountDetailTable.view(model)
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
