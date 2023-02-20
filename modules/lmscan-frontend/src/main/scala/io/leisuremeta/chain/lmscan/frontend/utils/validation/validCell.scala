package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import scala.util.matching.Regex
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}

enum Cell:
  case Image(data: Option[String])                              extends Cell
  case Head(data: String, css: String = "cell")                 extends Cell
  case Any(data: String, css: String = "cell")                  extends Cell
  case AGE(data: Option[Int])                                   extends Cell
  case DATE(data: Option[Int], css: String = "cell")            extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Int]))        extends Cell
  case BLOCK_HASH(data: Option[String])                         extends Cell
  case ACCOUNT_HASH(data: Option[String], css: String = "cell") extends Cell
  case ACCOUNT_HASH_DETAIL(data: Option[String], css: String = "cell")
      extends Cell
  case TX_HASH(data: Option[String])                    extends Cell
  case TX_HASH10(data: Option[String])                  extends Cell
  case Tx_VALUE(data: (Option[String], Option[String])) extends Cell
  case Tx_VALUE2(data: (Option[String], Option[String], Option[String]))
      extends Cell
  case PlainInt(data: Option[Int]) extends Cell
  case PlainStr(data: Option[String] | Option[Int], css: String = "cell")
      extends Cell
  case AAA(data: String) extends Cell

object gen:
  def cell(cells: Cell*) = cells
    .map(cell =>
      cell match
        case Cell.Image(nftUri) =>
          plainStr(nftUri).contains("img") match
            case true => // 이미지 포맷
              img(
                `class` := "nft-image p-10px",
                src     := s"${getOptionValue(nftUri, "-").toString()}",
              )

            case _ => // 비디오 포맷
              video(`class` := "nft-image p-10px", autoPlay, loop)(
                source(
                  src := s"https://d2t5puzz68k49j.cloudfront.net/release/collections/BPS_S.Younghoon/NFT_ITEM/DE8BB88B-48FB-4488-88BE-7F49894727AA.mp4",
                ),
              )
        case Cell.Head(data, css) => div(`class` := s"$css")(span()(data))
        case Cell.Any(data, css)  => div(`class` := s"$css")(span()(data))
        case Cell.PlainStr(data, css) =>
          div(`class` := s"$css")(span()(plainStr(data)))
        case Cell.PlainInt(data) =>
          div(`class` := "cell")(
            span(
            )(plainInt(data)),
          )
        case Cell.Tx_VALUE(tokeyType, value) =>
          div(
            `class` := s"cell ${isEqGet[String](plainStr(tokeyType), "NFT", "type-3")}",
          )(
            span(
              plainStr(tokeyType) match
                case "NFT" =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageName.NftDetail(
                        plainStr(value),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              plainStr(tokeyType) match
                case "NFT" =>
                  plainStr(value)
                case _ => txValue(value),
            ),
          )
        case Cell.Tx_VALUE2(tokeyType, value, inout) =>
          div(
            `class` := s"cell ${isEqGet[String](plainStr(tokeyType), "NFT", "type-3")}",
          )(
            {
              txValue(value) == "-" match
                case true => span()
                case false =>
                  span(
                    plainStr(inout) match
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
                  )(plainStr(inout))
            },
            span(
              plainStr(tokeyType) match
                case "NFT" =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageName.NftDetail(
                        plainStr(value),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              plainStr(tokeyType) match
                case "NFT" =>
                  plainStr(value)
                case _ => txValue(value),
            ),
          )
        case Cell.ACCOUNT_HASH(data, css) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.AccountDetail(
                    plainStr(data),
                  ),
                ),
              ),
            )(
              accountHash(data),
            ),
          )
        case Cell.ACCOUNT_HASH_DETAIL(data, css) =>
          div(`class` := s"$css")(
            span(
            )(
              accountHash_DETAIL(data),
            ),
          )
        case Cell.AGE(data) =>
          div(`class` := "cell")(
            span(
              dataAttr(
                "tooltip-text",
                Dom.yyyy_mm_dd_time(plainInt(data).toInt),
              ),
            )(
              Dom.timeAgo(
                plainInt(data).toInt,
              ),
            ),
          )

        case Cell.DATE(data, css) =>
          div(`class` := s"$css")(
            span(
            )(
              Dom.yyyy_mm_dd_time(
                plainInt(data).toInt,
              ),
            ),
          )

        case Cell.BLOCK_NUMBER((hash, number)) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.BlockDetail(
                    plainStr(hash),
                  ),
                ),
              ),
            )(plainInt(number)),
          )

        case Cell.BLOCK_HASH(data) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.BlockDetail(
                    plainStr(data),
                  ),
                ),
              ),
              dataAttr("tooltip-text", plainStr(data)),
            )(hash10(data)),
          )
        case Cell.TX_HASH(data) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    plainStr(data),
                  ),
                ),
              ),
            )(
              plainStr(data),
            ),
          )
        case Cell.TX_HASH10(data) =>
          div(`class` := "cell type-3")(
            span(
              dataAttr("tooltip-text", plainStr(data)),
              onClick(
                PageMsg.PreUpdate(
                  PageName.TransactionDetail(
                    plainStr(data),
                  ),
                ),
              ),
            )(
              hash10(data),
            ),
          )

        case _ => div(`class` := "cell")(span()("NO - CELL")),
    )
    .toList