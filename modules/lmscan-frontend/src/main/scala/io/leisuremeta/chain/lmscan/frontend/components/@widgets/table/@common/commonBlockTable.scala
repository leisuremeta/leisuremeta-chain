package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Row:
  def title = (model: Model) =>
    div(
      `class` := s"${State.css(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1 ",
      )(span()("Latest Blocks")),
      div(
        `class` := s" type-2 ",
      )(span(onClick(NavMsg.Blocks))("More")),
    )
  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Block")),
    div(`class` := "cell")(span()("Timestamp")),
    div(`class` := "cell")(span()("age")),
    div(`class` := "cell")(span()("Block Hash")),
    div(`class` := "cell")(span()("TX Count")),
  )
  val body = div(`class` := "row table-body")(
    div(`class` := "cell type-3")(span(onClick(NavMsg.BlockDetail))("123458")),
    div(`class` := "cell")(span()("YYYY-MM-DD HH:MM:SS")),
    div(`class` := "cell")(span()("1 year ago")),
    div(`class` := "cell")(span()("0x40e4c52e0d4340e2f")),
    div(`class` := "cell")(span()("123")),
  )
  val search = (model: Model) =>
    div(
      `class` := s"${State.css(model, NavMsg.DashBoard: Msg, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(`class` := "type-arrow")("<<"),
        div(`class` := "type-arrow")("<"),
        div(`class` := "type-plain-text")("Page"),
        div(`class` := "type-search")("1"),
        div(`class` := "type-plain-text")("of"),
        div(`class` := "type-plain-text")("6288700"),
        div(`class` := "type-arrow")(">"),
        div(`class` := "type-arrow")(">>"),
      ),
    )

object CommonBlockTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container ")(
      Row.title(model),
      div(`class` := "table w-[100%]")(
        Row.head,
        Row.body,
        Row.body,
        Row.body,
        Row.body,
        Row.body,
        Row.body,
      ),
      Row.search(model),
    )
