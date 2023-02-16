package io.leisuremeta.chain.lmscan.frontend
import Dom.*
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
        val from = getOptionValue(each.fromAddr, "-").toString()
        val to   = getOptionValue(each.toAddr, "-").toString()

        div(
          `class` := "row table-body",
        )(
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
            span()(
              yyyy_mm_dd_time(
                getOptionValue(each.createdAt, 0).asInstanceOf[Int],
              ),
            ),
          ),
          div(`class` := "cell")(
            span()(getOptionValue(each.action, "-").toString()),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.fromAddr,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(
              from.length match
                case 40 => from.take(10) + "..."
                case _ =>
                  from == "playnomm" match
                    case true =>
                      "010cd45939f064fd82403754bada713e5a9563a1".take(
                        10,
                      ) + "..."
                    case false => from,
            ),
          ),
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    getOptionValue(
                      each.toAddr,
                      "-",
                    )
                      .toString(),
                  ),
                ),
              ),
            )(to.length match
              case 40 => to.take(10) + "..."
              case _ =>
                to == "playnomm" match
                  case true =>
                    "010cd45939f064fd82403754bada713e5a9563a1".take(
                      10,
                    ) + "..."
                  case false => to,
            ),
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
