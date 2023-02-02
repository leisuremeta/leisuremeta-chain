package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet}

import Log.*

// TODO :: simplify
object Row3:
  def title = (model: Model) =>
    div(
      `class` := s"table-title ",
    )(
      div(
        `class` := s"type-1",
      )(span()("Item Activity")),
    )

  val head = div(`class` := "row table-head")(
    div(`class` := "cell")(span()("Tx Hash")), // hash
    div(`class` := "cell")(span()("Timestamp")), // blockNumber
    div(`class` := "cell")(span()("Action")),
    div(`class` := "cell")(span()("From")),
    div(`class` := "cell")(span()("To")),
  )

  def genBody = (payload: List[NftActivities]) =>
    payload
      .map(each =>
        div(`class` := "row table-body")(
          div(`class` := "cell type-3")(
            span(
              onClick(NavMsg.TransactionDetail(each.txHash)),
            )(each.txHash.take(10) + "..."),
          ),
          div(`class` := "cell")(span()(each.createdAt.toString())),
          div(`class` := "cell")(span()(each.action)),
          div(`class` := "cell")(span()(each.fromAddr)),
          div(`class` := "cell")(span()(each.toAddr)),
        ),
      )

  def genTable = (payload: List[NftActivities], model: Model) =>
    payload.isEmpty match
      case true => div()

      case _ =>
        div(`class` := "table-container")(
          Row3.title(model),
          div(`class` := "table w-[100%]")(
            Row3.head :: Row3.genBody(payload),
          ),
        )

  val table = (model: Model) =>
    NftDetailParser
      .decodeParser(model.nftDetailData.get)
      .map(data => Row3.genTable(data.activities, model))
      .getOrElse(div())

object NftTable:
  def view(model: Model): Html[Msg] =
    // div()("asdasd")
    Row3.table(model)
