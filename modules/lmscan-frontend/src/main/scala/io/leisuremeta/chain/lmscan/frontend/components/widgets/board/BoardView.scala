package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import Dom.*
import scala.util.chaining.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import io.leisuremeta.chain.lmscan.common.model.BlockInfo
import io.leisuremeta.chain.lmscan.frontend.Log.log2

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "TOTAL BALANCE"
  // val Transactions = "TOTAL DATA SIZE"
  val Accounts = "TOTAL ACCOUNTS"

object BoardView:
  def txValue(data: Option[String]) =
    val res = String
      .format(
        "%.2f",
        (getOptionValue(data, "0.0")
          .asInstanceOf[String]
          .toDouble / Math.pow(10, 18).toDouble),
      )
    val sosu         = res.takeRight(5)
    val decimal      = res.replace(sosu, "")
    val commaDecimal = String.format("%,d", decimal.toDouble)

    res == "0.0000" match
      case true =>
        "-"
      case false => commaDecimal + sosu

  def parseToNumber(strNum: String) =
    //  strNum.toDouble() / 1_000_000_000_000
    strNum.length() > 18 match
      case true =>
        String.format("%.0f", strNum.dropRight(18).toDouble)
      case false => String.format("%.0f", strNum.toDouble)

  def addComma(numberString: String) =
    numberString match
      case "-" => "-"
      case _   => f"${BigInt(numberString)}%,d"

  def view(model: Model): Html[Msg] =
    val data2 = get_PageResponseViewCase(model).board
    log2("data2")(data2)
    // val data = 1

    div(`class` := "board-area")(
      div(`class` := "board-list x ")(
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.LM_Price,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data2.lmPrice).take(6) + " USDT",
              // "as",
            ),
          ), {
            data2 != new SummaryModel match
              case false =>
                LoaderView.view(model)

              case _ => div()
          },
        ),
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Block_Number,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(
                current_ViewCase(model).blockInfo(0).number,
              ).pipe(addComma),
            ),
          ), {
            current_ViewCase(model).blockInfo(0) != new BlockInfo match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Transactions,
            ),
            div(`class` := "color-white font-bold")(
              {

                // String
                //   .format(
                //     "%.3f",
                //     plainLong(
                //       data.totalTxSize,
                //     ).toDouble / (1024 * 1024).toDouble,
                //   ) + " MB"

                // parseToNumber(
                //   data2
                //     .pipe(log2("data??"))
                //     .total_balance
                //     .map(_.toString)
                //     .getOrElse("0"),
                // ).pipe(addComma)
                "43,788,633.259"
              },
            ),
          ), {
            data2 != new SummaryModel match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
        div(`class` := "board-container xy-center position-relative  ")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(
              Board.Accounts,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data2.totalAccounts).pipe(addComma),
              // "as",
            ),
          ), {
            data2 != new SummaryModel match
              case false =>
                LoaderView.view(model)
              case _ => div()
          },
        ),
      ),
    )
