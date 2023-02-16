package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import scala.compiletime.ops.any
import ValidOutputData.*

object AccountDetailTable:
  val view = (model: Model) =>
    val data: AccountDetail = AccountDetailParser
      .decodeParser(model.accountDetailData.get)
      .getOrElse(new AccountDetail)
    genView(model, data)

  val genView = (model: Model, data: AccountDetail) =>
    val balance = getOptionValue(data.balance, 0.0)
      .asInstanceOf[Double] / Math.pow(10, 18).toDouble
    val value = Math.floor(
      getOptionValue(data.balance, 0.0).asInstanceOf[Double] * 10000,
    ) / 10000
    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(`class` := "type-TableDetail  table-container")(
          div(`class` := "table w-[100%] ")(
            div(`class` := "row")(
              div(`class` := "cell type-detail-head ")("Account"),
              div(`class` := "cell type-detail-body ")(
                getOptionValue(data.address, "-").toString(),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Balance"),
              div(`class` := "cell type-detail-body")(
                balance.toString() + "LM",
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Value"),
              div(`class` := "cell type-detail-body")(
                "$" + value.toString(),
              ),
            ),
          ),
        ),
      ),
    )
