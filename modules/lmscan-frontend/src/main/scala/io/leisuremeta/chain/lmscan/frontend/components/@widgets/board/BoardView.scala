package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Board:
  val LM_Price     = "LM Price"
  val Block_Number = "Block Number"
  val Transactions = "24h Transactions"
  val Accounts     = "Total Accounts"

  // TODO :: api data 로 변경 필요
  val LM_Price_value     = "0.394 USDT"
  val Block_Number_value = "12312123"
  val Transactions_value = "3,572,245"
  val Accounts_value     = "194,142,552"

object BoardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div()(Board.LM_Price),
            div()(Board.LM_Price_value),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div()(Board.Block_Number),
            div()(Board.Block_Number_value),
          ),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div()(Board.Transactions),
            div()(Board.Transactions_value),
          ),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text y-center gap-10px",
          )(
            div()(Board.Accounts),
            div()(Board.Accounts_value),
          ),
        ),
      ),
    )
