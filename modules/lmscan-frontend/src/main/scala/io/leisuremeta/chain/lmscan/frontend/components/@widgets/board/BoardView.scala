package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Board:
  val LM_Price     = "LM Price = 0.394 USDT"
  val Block_Number = "Block Number 21,872,421"
  val Transactions = "24h Transactions 3,572,245"
  val Accounts     = "Total Accounts 194,142,552"

object BoardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.LM_Price),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Block_Number),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Transactions),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Accounts),
        ),
      ),
    )
