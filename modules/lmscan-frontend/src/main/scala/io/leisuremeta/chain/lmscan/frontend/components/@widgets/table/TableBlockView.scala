package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Row:
  def title = (model: Model) =>
    div(`class` := "table-title")(
      div(`class` := "type-1")(span()("Latest Blocks")),
      div(
        `class` := s"state type-2 ${model.curPage.toString() == NavMsg.DashBoard.toString()}",
      )(span(onClick(NavMsg.Blocks))("More")),
    )
  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Block")),
    div(`class` := "cell")(span()("Timestamp")),
    div(`class` := "cell")(span()("Block Hash")),
    div(`class` := "cell")(span()("TX Count")),
  )
  val body = div(`class` := "row table-body")(
    div(`class` := "cell type-3")(span(onClick(NavMsg.BlockDetail))("123458")),
    div(`class` := "cell")(span()("YYYY-MM-DD HH:MM:SS")),
    div(`class` := "cell")(span()("0x40e4c52e0d4340e2f")),
    div(`class` := "cell")(span()("123")),
  )
  val search = (model: Model) =>
    div(`class` := s"state table-search xy-center ${model.curPage
        .toString() == NavMsg.DashBoard.toString()}")(
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

object TableBlockView:
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
