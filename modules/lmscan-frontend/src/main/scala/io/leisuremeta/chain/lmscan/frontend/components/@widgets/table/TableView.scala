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
      div(id := "table-blocks", `class` := "table-list x")(
        div(`class` := "table-container xy-center")(
          table()(
            thead()(
              tr()(
                td()(span()("Block")),
                td()(span()("Timestamp")),
                td()(span()("Block Hash")),
                td()(span()("TX Count")),
              ),
            ),
            tbody()(
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
            ),
          ),
        ),
        div(`class` := "table-container xy-center")(
          table()(
            thead()(
              tr()(
                td()(span()("Block")),
                td()(span()("Timestamp")),
                td()(span()("Block Hash")),
                td()(span()("TX Count")),
              ),
            ),
            tbody()(
              tr()(
                td()(span()("1234")),
                td()(span()("YYYY-MM-DD HH:MM:SS")),
                td()(span()("0x40e4c52e")),
                td()(span()("123")),
              ),
            ),
          ),
        ),
      ),
    )
