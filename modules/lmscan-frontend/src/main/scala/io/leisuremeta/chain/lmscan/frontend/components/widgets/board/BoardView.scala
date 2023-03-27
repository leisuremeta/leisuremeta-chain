package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.Log.log

object Board:
  val LM_Price     = "LM PRICE"
  val Block_Number = "BLOCK NUMBER"
  val Transactions = "24H TRANSACTIONS"
  val Accounts     = "TOTAL ACCOUNTS"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data = get_PageResponseViewCase(model).board
    // val data = new SummaryModel
    log(data)

    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold")(Board.LM_Price),
            div(`class` := "color-white font-bold")(
              plainStr(data.lmPrice).take(6) + " USDT",
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Block_Number,
            ),
            div(`class` := "color-white font-bold")(
              {
                log("get_ViewCase(model).blockInfo")
                log(get_ViewCase(model).blockInfo)
                // log(get_ViewCase(model).blockInfo(0))
                // plainStr(get_ViewCase(model).blockInfo(0).number), //조회 안될때 에러를 내뱉음
                "as"
              },
            ),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Transactions,
            ),
            div(`class` := "color-white font-bold")(
              // getOptionValue(data.txCountInLatest24h, "-").toString(),
              // String.format(
              //   "%,d",
              //   getOptionValue(data.txCountInLatest24h, "-").toString().toDouble,
              // ),
              plainStr(data.txCountInLatest24h),
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-white font-bold color-white")(
              Board.Accounts,
            ),
            div(`class` := "color-white font-bold")(
              plainStr(data.totalAccounts),
              // "39,104",
            ),
          ),
        ),
      ),
    )
