package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden}

import Log.*

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
    div(`class` := "cell")(span()("Tx Hash")), // hash
    div(`class` := "cell")(span()("Block")), // blockNumber
    div(`class` := "cell")(span()("Age")), // createdAt
    div(`class` := "cell")(span()("Signer")), // signer
    div(`class` := "cell")(span()("Type")), // txType
    div(`class` := "cell")(span()("Token Type")),
    div(`class` := "cell")(span()("value")),
  )
  def genBody = (payload: List[Tx]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(
              onClick(NavMsg.TransactionDetail(each.hash)),
            )(each.hash.take(10) + "..."),
          ),
          div(`class` := "cell")(span()(each.blockNumber.toString())),
          div(`class` := "cell")(span()(each.createdAt.toString())),
          div(`class` := "cell")(span()(each.signer.take(10) + "...")),
          div(`class` := "cell")(span()(each.txType)),
          div(`class` := "cell")(span()(each.tokenType)),
          div(`class` := "cell")(span()(each.value.take(14) + "...")),
        ),
      )

  def genTable = (payload: List[Tx]) =>
    div(`class` := "table w-[100%]")(
      Row2.head :: Row2.genBody(payload),
    )

  val table = (model: Model) =>
    TxParser
      .decodeParser(model.txListData.get)
      .map(data => Row2.genTable(data.payload))
      .getOrElse(div())

  val search = (model: Model) =>
    div(
      `class` := s"${State.curPage(model, NavMsg.DashBoard, "_search")} table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch("1")),
        )("<<"),
        div(
          `class` := s"type-arrow ${_hidden[Int](1, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Prev),
        )("<"),
        div(`class` := "type-plain-text")("Page"),
        input(
          onInput(s => PageMoveMsg.Get(s)),
          value   := s"${model.tx_list_Search}",
          `class` := "type-search xy-center DOM-page1 ",
        ),
        div(`class` := "type-plain-text")("of"),
        div(`class` := "type-plain-text")(model.tx_TotalPage.toString()),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Next),
        )(">"),
        div(
          `class` := s"type-arrow ${_hidden[Int](model.tx_TotalPage, model.tx_CurrentPage)}",
          onClick(PageMoveMsg.Patch(model.tx_TotalPage.toString())),
        )(">>"),
      ),
    )

object TransactionTable:
  def view(model: Model): Html[Msg] =
    div(`class` := "table-container")(
      Row2.title(model),
      Row2.table(model),
      Row2.search(model),
    )
