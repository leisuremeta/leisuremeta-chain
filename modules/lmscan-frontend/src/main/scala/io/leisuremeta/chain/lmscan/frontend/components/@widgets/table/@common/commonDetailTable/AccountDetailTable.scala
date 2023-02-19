package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import _root_.io.circe.Decoder.state
import scala.compiletime.ops.any
import W.*
import java.math.RoundingMode

object AccountDetailTable:
  val view = (model: Model) =>
    val apiData: ApiData =
      ApiParser.decodeParser(model.apiData.get).getOrElse(new ApiData)
    val data: AccountDetail = AccountDetailParser
      .decodeParser(model.accountDetailData.get)
      .getOrElse(new AccountDetail)
    genView(model, data, apiData)

  val genView = (model: Model, data: AccountDetail, apiData: ApiData) =>
    val lmPrice = Math.floor(
      getOptionValue(apiData.lmPrice, 0.0).asInstanceOf[Double] * 10000,
    ) / 10000
    val balance = getOptionValue(data.balance, 0.0)
      .asInstanceOf[Double] / Math.pow(10, 18).toDouble
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
              div(`class` := "cell type-detail-head ")("Account"),
              div(`class` := "cell type-detail-body ")(
                getOptionValue(data.address, "-").toString().length match
                  case 40 =>
                    getOptionValue(data.address, "-")
                      .toString()

                  case _ =>
                    getOptionValue(data.address, "-")
                      .toString() match
                      case "playnomm" =>
                        "010cd45939f064fd82403754bada713e5a9563a1"
                      case "eth-gateway" =>
                        "ca79f6fb199218fa681b8f441fefaac2e9a3ead3"
                      case _ =>
                        getOptionValue(data.address, "-").toString(),
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Balance"),
              div(`class` := "cell type-detail-body")(
                formattedBalance.toString() + " LM",
              ),
            ),
            div(`class` := "row")(
              div(`class` := "cell type-detail-head")("Value"),
              div(`class` := "cell type-detail-body")(
                "$ " + formattedValue.toString(),
              ),
            ),
          ),
        ),
      ),
    )
