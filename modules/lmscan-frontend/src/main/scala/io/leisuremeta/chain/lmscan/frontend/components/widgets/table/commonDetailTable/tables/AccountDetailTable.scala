package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import scala.compiletime.ops.any
import V.*
import java.math.RoundingMode
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.common.model.AccountDetail
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*

object AccountDetailTable:
  val view = (model: Model) =>
    val apiData: SummaryModel = get_PageResponseViewCase(model).board

    val data: AccountDetail = get_PageResponseViewCase(model).accountDetail
    genView(model, data, apiData)

    // val data: AccountDetail = AccountDetailParser
    //   .decodeParser(model.accountDetailData.get)
    //   .getOrElse(new AccountDetail)
    // genView(model, data, apiData)

  val genView = (model: Model, data: AccountDetail, apiData: SummaryModel) =>
    val lmPrice = Math.floor(
      getOptionValue(apiData.lmPrice, 0.toDouble).asInstanceOf[Double] * 10000,
    ) / 10000
    val balance = getOptionValue[BigDecimal](data.balance, 0)
      .asInstanceOf[BigDecimal] / Math.pow(10, 18).toDouble
    val value = (lmPrice * balance)
    // val value   = Math.floor((lmPrice * balance) * 10000) / 10000

    val formatter = java.text.NumberFormat.getNumberInstance()
    formatter.setRoundingMode(RoundingMode.FLOOR)

    formatter.setMaximumFractionDigits(18)
    val formattedBalance = formatter.format(balance)

    formatter.setMaximumFractionDigits(4)
    val formattedValue = formatter.format(value)

    div(`class` := "y-start gap-10px w-[100%] ")(
      div(`class` := "x")(
        div(`class` := "type-TableDetail  table-container")(
          div(`class` := "table w-[100%] ")(
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Account", "cell type-detail-head"),
                Cell.PlainStr(data.address, "cell type-detail-body"),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Balance", "cell type-detail-head"),
                Cell.Any(
                  formattedBalance.toString() + " LM",
                  "cell type-detail-body",
                ),
              ),
            ),
            div(`class` := "row")(
              gen.cell(
                Cell.Head("Value", "cell type-detail-head"),
                Cell.Any(
                  "$ " + formattedValue.toString(),
                  "cell type-detail-body",
                ),
              ),
            ),
          ),
        ),
      ),
    )
