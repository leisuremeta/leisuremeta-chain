package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object BoardView:
  def view(model: Model): Html[Msg] =
    div(`class` := "board-area")(
      div(`class` := "board-list center")(
        div(`class` := "board-container center")(
          div(
            `class` := "board-text center",
          )("LM Price = 0.394 USDT"),
        ),
        div(`class` := "board-container center")(
          div(
            `class` := "board-text center",
          )("Block Number 21,872,421"),
        ),
      ),
      div(`class` := "board-list center")(
        div(`class` := "board-container center")(
          div(
            `class` := "board-text center",
          )("24h Transactions 3,572,245"),
        ),
        div(`class` := "board-container center")(
          div(
            `class` := "board-text center",
          )("Total Accounts 194,142,552"),
        ),
      ),
    )
