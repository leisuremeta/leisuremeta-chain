package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

// object Table:
//   val LM_Price     = "LM Price = 0.394 USDT"
//   val Block_Number = "Block Number 21,872,421"
//   val Transactions = "24h Transactions 3,572,245"
//   val Accounts     = "Total Accounts 194,142,552"

object Row:
  val title = div(`class` := "row table-title ")(
    div(`class` := "cell type-1")(span()("최신블록")),
    div(`class` := "cell")(span()("")),
    div(`class` := "cell")(span()("")),
    div(`class` := "cell type-2")(span()("더 보기")),
  )
  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Block")),
    div(`class` := "cell")(span()("Timestamp")),
    div(`class` := "cell")(span()("Block Hash")),
    div(`class` := "cell")(span()("TX Count")),
  )
  val body = div(`class` := "row table-body")(
    div(`class` := "cell type-3")(span()("123458")),
    div(`class` := "cell")(span()("YYYY-MM-DD HH:MM:SS")),
    div(`class` := "cell")(span()("0x40e4c52e0d4340e2f")),
    div(`class` := "cell")(span()("123")),
  )

object TableView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-area")(
      div(id := "oop-table-blocks", `class` := "table-list x")(
        div(`class` := "table table-container")(
          Row.title,
          Row.head,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
        ),
        div(`class` := "table table-container")(
          Row.title,
          Row.head,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
          Row.body,
        ),
      ),
    )
