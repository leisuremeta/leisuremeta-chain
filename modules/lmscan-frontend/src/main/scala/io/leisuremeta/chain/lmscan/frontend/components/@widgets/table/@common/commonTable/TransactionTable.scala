package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Row2:
  def title = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest transactions")),
      div(
        `class` := s"type-2",
      )(span(onClick(NavMsg.Transactions))("More")),
    )
  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("TX Hash")),
    div(`class` := "cell")(span()("Block")),
    div(`class` := "cell")(span()("Age")),
    div(`class` := "cell")(span()("Signer")),
    div(`class` := "cell")(span()("Type")),
    div(`class` := "cell")(span()("Token Type")),
    div(`class` := "cell")(span()("Value")),
  )
  val body = div(`class` := "row table-body")(
    div(`class` := "cell type-3")(
      span(onClick(NavMsg.TransactionDetail))("bcf186a5ed..."),
    ),
    div(`class` := "cell")(span()("123,456,789")),
    div(`class` := "cell")(span()("5s ago")),
    div(`class` := "cell type-3")(
      span(onClick(NavMsg.Account))("73c7e699d9..."),
    ),
    div(`class` := "cell")(span()("Account")),
    div(`class` := "cell")(span()("NFT")),
    div(`class` := "cell type-3")(span(onClick(NavMsg.Nft))("123,12412123 LM")),
  )

object TransactionTable:
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
