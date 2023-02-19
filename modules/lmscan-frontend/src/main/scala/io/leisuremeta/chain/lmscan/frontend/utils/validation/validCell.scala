package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import scala.util.matching.Regex
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}

enum Cell:
  case Head(data: String)                                extends Cell
  case AGE(data: Option[Int])                            extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Int])) extends Cell
  case BLOCK_HASH(data: Option[String])                  extends Cell
  case ACCOUNT_HASH(data: Option[String])                extends Cell
  case TX_HASH(data: Option[String])                     extends Cell
  case TX_HASH10(data: Option[String])                   extends Cell
  case Tx_VALUE(data: (Option[String], Option[String]))  extends Cell
  case Tx_VALUE2(data: (Option[String], Option[String], Option[String]))
      extends Cell
  case PlainInt(data: Option[Int])    extends Cell
  case PlainStr(data: Option[String]) extends Cell
  case AAA(data: String)              extends Cell

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
        case Cell.Tx_VALUE(tokeyType, value) =>
          div(
            `class` := s"cell ${isEqGet[String](W.plainStr(tokeyType), "NFT", "type-3")}",
          )(
            span(
              W.plainStr(tokeyType) match
                case "NFT" =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageName.NftDetail(
                        W.plainStr(value),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              W.plainStr(tokeyType) match
                case "NFT" =>
                  W.plainStr(value)
                case _ => W.txValue(value),
            ),
          )
        case Cell.Tx_VALUE2(tokeyType, value, inout) =>
          div(
            `class` := s"cell ${isEqGet[String](W.plainStr(tokeyType), "NFT", "type-3")}",
          )(
            {
              W.txValue(value) == "-" match
                case true => span()
                case false =>
                  span(
                    W.plainStr(inout) match
                      case "In" =>
                        List(
                          style(
                            Style(
                              "background-color" -> "white",
                              "padding"          -> "5px",
                              "border"           -> "1px solid green",
                              "border-radius"    -> "5px",
                              "margin-right"     -> "5px",
                            ),
                          ),
                        )
                      case "Out" =>
                        List(
                          style(
                            Style(
                              "background-color" -> "rgba(171, 242, 0, 0.5)",
                              "padding"          -> "5px",
                              "border"           -> "1px solid green",
                              "border-radius"    -> "5px",
                              "margin-right"     -> "5px",
                            ),
                          ),
                        ),
                  )(W.plainStr(inout))
            },
            span(
              W.plainStr(tokeyType) match
                case "NFT" =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageName.NftDetail(
                        W.plainStr(value),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              W.plainStr(tokeyType) match
                case "NFT" =>
                  W.plainStr(value)
                case _ => W.txValue(value),
            ),
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
        case Cell.TX_HASH10(data) =>
          div(`class` := "cell type-3")(
            span(
              dataAttr("tooltip-text", W.plainStr(data)),
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    W.plainStr(data),
                  ),
                ),
              ),
            )(
              W.hash10(data),
            ),
          )

        case _ => div(`class` := "cell")(span()("NO - CELL")),
    )
    .toList
