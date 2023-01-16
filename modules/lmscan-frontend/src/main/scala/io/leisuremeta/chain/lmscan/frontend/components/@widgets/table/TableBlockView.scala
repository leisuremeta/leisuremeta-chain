package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*

object Row:
  def title = (model: Model) =>
    div(`class` := "row table-title ")(
      div(`class` := "cell type-1")(span()("Latest Blocks")),
      // div(`class` := "cell type-1")(span()("최신 블록")),
      div(`class` := "cell")(span()("")),
      div(`class` := "cell")(span()("")),
      // div(`class` := "cell type-2")(span(onClick(NavMsg.Blocks))("더 보기")),
      div(
        `class` := s"state cell type-2 ${model.curPage.toString() == NavMsg.DashBoard.toString()}",
      )(span(onClick(NavMsg.Blocks))("More")),
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
  val search = div(`class` := "table-search")(
    div(`class` := "")(
      span()("<<"),
      span()("<"),
      span()("Page"),
      span()("1"),
      span()("of"),
      span()("6288700"),
      span()(">"),
      span()(">>"),
    ),
  )

object TableBlockView:
  def view(model: Model): Html[Msg] =
    div(`class` := "table table-container")(
      Row.title(model),
      Row.head,
      Row.body,
      Row.body,
      Row.body,
      Row.body,
      Row.body,
      Row.body,
      Row.search,
    )
