package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.util.matching.Regex

enum Cell:
  case Head(data: String)                                extends Cell
  case AGE(data: Option[Int])                            extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Int])) extends Cell
  case BLOCK_HASH(data: Option[String])                  extends Cell
  case PlainInt(data: Option[Int])                       extends Cell
  case PlainStr(data: Option[String])                    extends Cell
  case AAA(data: String)                                 extends Cell

object W:
  def getOptionValue = (field: Option[Any], default: Any) =>
    field match
      case Some(value) => value
      case None        => default

  def plainStr(data: Option[String]) =
    getOptionValue(data, "-").toString()

  def plainInt(data: Option[Int]) =
    getOptionValue(data, 0).asInstanceOf[Int].toString

  def hash(data: Option[Any]) = getOptionValue(data, "-").toString()

  def hash10(data: Option[Any]) =
    getOptionValue(data, "-").toString().take(10) + "..."

  def _any(data: Option[Any]) = ""

object gen:

  def cell(cells: Cell*) = cells
    .map(cell =>
      cell match
        case Cell.Head(data) => div(`class` := "cell")(span()(data))

        case Cell.AGE(data) =>
          div(`class` := "cell")(
            span(
              dataAttr(
                "tooltip-text",
                Dom.yyyy_mm_dd_time(W.plainInt(data).toInt),
              ),
            )(
              Dom.timeAgo(
                W.plainInt(data).toInt,
              ),
            ),
          )

        case Cell.BLOCK_NUMBER((hash, number)) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.BlockDetail(
                    W.plainStr(hash),
                  ),
                ),
              ),
            )(W.plainInt(number)),
          )

        case Cell.BLOCK_HASH(data) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.BlockDetail(
                    W.plainStr(data),
                  ),
                ),
              ),
              dataAttr("tooltip-text", W.plainStr(data)),
            )(W.hash10(data)),
          )
        case Cell.PlainInt(data) =>
          div(`class` := "cell")(
            span(
            )(W.plainInt(data)),
          )
        case _ => div(`class` := "cell")(span()("NO - CELL")),
    )
    .toList
