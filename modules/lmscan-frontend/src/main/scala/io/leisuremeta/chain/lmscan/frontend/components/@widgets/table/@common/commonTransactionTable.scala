package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Row2:
  def title = (model: Model) =>
    div(
      `class` := s"${State.css(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(span(onClick(NavMsg.Transactions))("More")),
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

object CommonTransactionTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Row2.title(model),
      div(`class` := "table w-[100%]")(
        Row2.head,
        Row2.body,
        Row2.body,
        Row2.body,
        Row2.body,
        Row2.body,
        Row2.body,
      ),
      Row.search(model),
    )
