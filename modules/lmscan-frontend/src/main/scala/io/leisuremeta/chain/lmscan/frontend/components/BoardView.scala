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

object BoardView:
  val LM_Price = "LM PRICE"
  val Total_TxCount = "TOTAL TRANSACTIONS"
  val Transactions  = "TOTAL BALANCE"
  val Accounts = "TOTAL ACCOUNTS"
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
      case true => "-"
      case false => commaDecimal + sosu

  def parseToNumber(strNum: String) =
    strNum.length > 18 match
      case true =>
        f"${BigDecimal(strNum) / Math.pow(10, 18)}%,.3f"
      case false => String.format("%.0f", strNum.toDouble)

  def addComma(numberString: String) =
    numberString match
      case "-" => "-"
      case _   => f"${BigInt(numberString)}%,d"
  

  def view(model: Model): Html[Msg] =
    val data = get_PageResponseViewCase(model).board
    println(model.appStates.last)
    println(model.appStates.length)

    div(`class` := "board-area")(
      List(
        (LM_Price, plainStr(data.lmPrice).take(6) + " USDT"),
        (Total_TxCount, plainLong(data.totalTxCount).pipe(addComma)),
        (Transactions, parseToNumber(data.total_balance.map(_.toString).getOrElse("0"))),
        (Accounts, plainStr(data.totalAccounts).pipe(addComma)),
      ).map(v => 
        div(`class` := "board-container xy-center position-relative")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(v._1),
            div(`class` := "color-white font-bold")(v._2),
          ), {
            div()
              // LoaderView.view(model)
            // data != SummaryModel match
            //   case false => LoaderView.view(model)
            //   case _ => div()
          },
        )
      )
    )
