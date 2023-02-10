package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import ValidOutputData.*

object Board:
  val LM_Price     = "LM Price"
  val Block_Number = "Block Number"
  val Transactions = "24h Transactions"
  val Accounts     = "Total Accounts"

object BoardView:
  def view(model: Model): Html[Msg] =
    val data: ApiData =
      ApiParser.decodeParser(model.apiData.get).getOrElse(new ApiData)

    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.LM_Price),
            div()(
              getOptionValue(data.lmPrice, "-").toString().take(6) + " USDT",
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Block_Number),
            div()(getOptionValue(data.blockNumber, "-").toString()),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Transactions),
            div()(
              getOptionValue(data.txCountInLatest24h, "-").toString(),
            ),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div(`class` := "font-16px color-grey")(Board.Accounts),
            div()(getOptionValue(data.totalAccounts, "-").toString()),
          ),
        ),
      ),
    )
