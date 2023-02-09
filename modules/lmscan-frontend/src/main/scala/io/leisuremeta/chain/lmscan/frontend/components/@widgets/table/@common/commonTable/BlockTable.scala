package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, timeAgo}

import Log.*

object Row:
  def title = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard: Msg, "_table-title")} table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Latest Blocks")),
      div(
        `class` := s"type-2",
      )(span(onClick(NavMsg.Blocks))("More")),
    )

  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Block")), // hash
    div(`class` := "cell")(span()("Age")), // createdAt
    div(`class` := "cell")(span()("Block hash")), // number
    div(`class` := "cell")(span()("Tx count")), // txCount
  )

  def genBody = (payload: List[Block]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(onClick(NavMsg.BlockDetail(CommonFunc.getOptionValue(each.hash, "-").toString())))(
              CommonFunc.getOptionValue(each.hash, "-").toString().take(10) + "...",
            ),
          ),
          div(`class` := "cell")(span()(timeAgo(CommonFunc.getOptionValue(each.createdAt, 0).asInstanceOf[Int]))),
          div(`class` := "cell")(span()(CommonFunc.getOptionValue(each.number, "-").toString())),
          div(`class` := "cell")(span()(CommonFunc.getOptionValue(each.txCount, "-").toString())),
        ),
      )

  def genTable = (payload: List[Block]) =>
    div(`class` := "table w-[100%]")(
      Row.head :: Row.genBody(payload),
    )

  val table = (model: Model) =>
    val data: BlockList = BlockParser.decodeParser(model.blockListData.get).getOrElse(new BlockList)      
    val payload = CommonFunc.getOptionValue(data.payload, List()).asInstanceOf[List[Block]]
    Row.genTable(payload)      

  val search = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard, "_search")} table-search xy-center ",
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
    div(`class` := "table-container")(
      Row.title(model),
      Row.table(model),
      Row.search(model),
    )
