package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.circe.*, io.circe.parser.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import Dom.{_hidden, isEqGet}
import ValidOutputData.*

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
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    getOptionValue(each.txHash, "-").toString(),
                  ),
                ),
              ),
            )(
              getOptionValue(each.txHash, "-")
                .toString(),
                // .take(10) + "...",
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.createdAt, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.action, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.fromAddr, "-").toString()),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.toAddr, "-").toString()),
          ),
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
    val data: NftDetail = NftDetailParser
      .decodeParser(model.nftDetailData.get)
      .getOrElse(new NftDetail)
    val activities = getOptionValue(data.activities, List())
      .asInstanceOf[List[NftActivities]]
    Row3.genTable(activities, model)

object NftTable:
  def view(model: Model): Html[Msg] =
    Row3.table(model)
