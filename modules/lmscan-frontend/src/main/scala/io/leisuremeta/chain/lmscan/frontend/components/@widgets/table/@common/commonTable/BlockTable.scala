package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import Dom.{_hidden}
import sttp.tapir.Schema.annotations.hidden

object Row:
  def title = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1 ",
      )(span()("Latest Blocks")),
      div(
        `class` := s"type-2",
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
      `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.block_CurrentPage)}",
          onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.block_CurrentPage)}",
          onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := "type-plain-text")("Page"),
        input(
          onInput(s => PageMoveMsg.Get(s)),
          value   := s"${model.block_list_Search}",
          `class` := "type-search xy-center DOM-page1 ",
        ),
        div(`class` := "type-plain-text")("of"),
        div(`class` := "type-plain-text")(model.block_TotalPage.toString()),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.block_TotalPage, model.block_CurrentPage)}",
          onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.block_TotalPage, model.block_CurrentPage)}",
          onClick(PageMoveMsg.Patch(model.block_TotalPage.toString())),
        )(">>"),
      ),
    )

object BlockTable:
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
