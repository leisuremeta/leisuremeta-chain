package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.util.matching.Regex

enum Cell:
  case Head(data: String)                                extends Cell
  case AGE(data: Option[Int])                            extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Int])) extends Cell
  case BLOCK_HASH(data: Option[String])                  extends Cell
  case ACCOUNT_HASH(data: Option[String])                extends Cell
  case TX_HASH(data: Option[String])                     extends Cell
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

  def accountHash(data: Option[String]) =
    plainStr(data).length match
      case 40 =>
        plainStr(data)
          .take(10) + "..."
      case _ =>
        plainStr(data).toString() match
          case "playnomm" =>
            "010cd45939f064fd82403754bada713e5a9563a1".take(
              10,
            ) + "..."
          case "eth-gateway" =>
            "ca79f6fb199218fa681b8f441fefaac2e9a3ead3".take(
              10,
            ) + "..."
          case _ =>
            plainStr(data)

  def hash10(data: Option[Any]) =
    getOptionValue(data, "-").toString().take(10) + "..."

  def _any(data: Option[Any]) = ""

object gen:

  def cell(cells: Cell*) = cells
    .map(cell =>
      cell match
        case Cell.Head(data) => div(`class` := "cell")(span()(data))
        case Cell.PlainStr(data) =>
          div(`class` := "cell")(span()(W.plainStr(data)))
        case Cell.PlainInt(data) =>
          div(`class` := "cell")(
            span(
            )(W.plainInt(data)),
          )
        case Cell.ACCOUNT_HASH(data) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    W.plainStr(data),
                  ),
                ),
              ),
            )(
              W.accountHash(data),
            ),
          )
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
        case Cell.TX_HASH(data) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    W.plainStr(data),
                  ),
                ),
              ),
            )(
              W.plainStr(data),
            ),
          )

        case _ => div(`class` := "cell")(span()("NO - CELL")),
    )
    .toList
