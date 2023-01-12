package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Table:
  val LM_Price     = "LM Price = 0.394 USDT"
  val Block_Number = "Block Number 21,872,421"
  val Transactions = "24h Transactions 3,572,245"
  val Accounts     = "Total Accounts 194,142,552"

object TableView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(`class` := "table-list x")(
        div(`class` := "table-container xy-center")(
        ),
        div(`class` := "table-container xy-center")(
        ),
      ),
      div(`class` := "table-list x")(
        div(`class` := "table-container xy-center")(
        ),
        div(`class` := "table-container xy-center")(
        ),
      ),
    )
