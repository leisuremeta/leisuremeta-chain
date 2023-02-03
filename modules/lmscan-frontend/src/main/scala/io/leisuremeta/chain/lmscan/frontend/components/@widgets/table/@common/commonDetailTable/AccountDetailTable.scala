package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state

object AccountDetailTable:
  val view = (model: Model) =>
    AccountDetailParser
      .decodeParser(model.accountDetailData.get)
      .map(data => genView(model, data))
      .getOrElse(div())

  val genView = (model: Model, data: AccountDetail) =>
    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(`class` := "type-TableDetail  table-container")(
          div(`class` := "table w-[100%] ")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head ")("Account"),
              div(`class` := "cell type-detail-body ")(data.address),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Balance"),
              div(`class` := "cell type-detail-body")(data.balance.toString + "LM"),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Value"),
              div(`class` := "cell type-detail-body")("$" + data.value.toString),
            ),
          ),
        ),
      ),
    )    
