package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import scala.util.matching.Regex
import Dom.{_hidden, isEqGet, yyyy_mm_dd_time, timeAgo}
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.Log.*

enum Cell:
  case Image(data: Option[String])                              extends Cell
  case Head(data: String, css: String = "cell")                 extends Cell
  case Any(data: String, css: String = "cell")                  extends Cell
  case AGE(data: Option[Long])                                  extends Cell
  case DATE(data: Option[Long], css: String = "cell")           extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Long]))       extends Cell
  case BLOCK_HASH(data: Option[String])                         extends Cell
  case ACCOUNT_HASH(data: Option[String], css: String = "cell") extends Cell
  case ACCOUNT_HASH_DETAIL(data: Option[String], css: String = "cell")
      extends Cell
  case TX_HASH(data: Option[String])                    extends Cell
  case TX_HASH10(data: Option[String])                  extends Cell
  case Tx_VALUE(data: (Option[String], Option[String])) extends Cell
  case Tx_VALUE2(data: (Option[String], Option[String], Option[String]))
      extends Cell
  case PlainInt(data: Option[Int])   extends Cell
  case PlainLong(data: Option[Long]) extends Cell
  case PlainStr(
      data: Option[String] | Option[Int] | Option[Long],
      css: String = "cell",
  )                      extends Cell
  case AAA(data: String) extends Cell

object gen:
  def cell(cells: Cell*) = cells
    .map(cell =>
      cell match
        case Cell.Image(nftUri) =>
          List("mp3", "mp4")
            .map(data => plainStr(nftUri).contains(data))
            .contains(true) match
            case true => // 비디오 포맷
              video(
                `class` := "nft-image p-10px",
                autoPlay,
                loop,
                name := "media",
              )(
                source(
                  src    := s"${getOptionValue(nftUri, "-").toString()}",
                  `type` := "video/mp4",
                ),
              )
            case _ => // 이미지 포맷
              img(
                `class` := "nft-image p-10px",
                src     := s"${getOptionValue(nftUri, "-").toString()}",
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
        case Cell.PlainLong(data) =>
          div(`class` := "cell")(
            span(
            )(plainLong(data)),
          )
        case Cell.Tx_VALUE(subType, value) =>
          div(
            `class` := s"cell ${plainStr(subType).contains("Nft") match
                case true => "type-3"
                case _    => ""
              }",
          )(
            span(
              plainStr(subType).contains("Nft") match
                case true =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageCase.NftDetail(
                        name = PageCase.AccountDetail().name,
                        url = s"nft/${plainStr(value)}",
                        pubs = List(
                          PubCase.NftDetailPub(
                            hash = plainStr(value),
                          ),
                        ),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              plainStr(subType).contains("Nft") match
                case true =>
                  plainStr(value)
                case _ =>
                  value
                    .map(s => s.forall(Character.isDigit))
                    .getOrElse(false) match
                    case true =>
                      txValue(value)
                    case false => plainStr(value),
            ),
          )
        case Cell.Tx_VALUE2(subType, value, inout) =>
          div(
            `class` := s"cell ${plainStr(subType).contains("Nft") match
                case true => "type-3"
                case _    => ""
              }",
          )(
            {
              txValue(value) == "-" || {
                plainStr(value) == "-"
              } match
                case true => span()
                case false =>
                  span(
                    plainStr(inout) match
                      case "In" =>
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
                        )
                      case "Out" =>
                        List(
                          style(
                            Style(
                              "text-decoration"  -> "none",
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
              plainStr(subType).contains("Nft") match
                case true =>
                  onClick(
                    PageMsg.PreUpdate(
                      PageCase.NftDetail(
                        name = PageCase.AccountDetail().name,
                        url = s"nft/${plainStr(value)}",
                        pubs = List(
                          PubCase.NftDetailPub(
                            hash = plainStr(value),
                          ),
                        ),
                      ),
                    ),
                  )
                case _ => EmptyAttribute,
            )(
              plainStr(subType).contains("Nft") match
                case true =>
                  plainStr(value).replace("-", "")
                case _ => txValue(value),
            ),
          )
        case Cell.ACCOUNT_HASH(hash, css) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageCase.AccountDetail(
                    name = PageCase.AccountDetail().name,
                    url = s"account/${plainStr(hash)}",
                    pubs = List(
                      PubCase.BoardPub(1, "", SummaryModel()),
                      PubCase.AccountDetailPub(hash = plainStr(hash)),
                      PubCase.TxPub(
                        page = 1,
                        accountAddr = plainStr(hash),
                        sizePerRequest = 10,
                      ),
                    ),
                  ),
                ),
              ),
            )(
              accountHash(hash),
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
                Dom.yyyy_mm_dd_time(plainLong(data).toInt),
              ),
            )(
              {
                val age = Dom.timeAgo(
                  plainLong(data).toInt,
                )
                age match
                  case x if x.contains("53 years") => "-"
                  case _                           => age
              },
            ),
          )

        case Cell.DATE(data, css) =>
          div(`class` := s"$css")(
            span(
            )(
              {
                val date = Dom.yyyy_mm_dd_time(
                  plainLong(data).toInt,
                ) + " +UTC"
                date match
                  case x if x.contains("1970") => "-"
                  case _                       => date

              },
            ),
          )

        case Cell.BLOCK_NUMBER((hash, number)) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageCase.BlockDetail(
                    name = PageCase.Blocks().name,
                    url = s"block/${plainStr(hash)}",
                    pubs = List(
                      PubCase.BlockDetailPub(
                        hash = plainStr(hash),
                      ),
                      PubCase.TxPub(
                        page = 1,
                        blockHash = plainStr(hash),
                        sizePerRequest = 10,
                      ),
                    ),
                  ),
                ),
              ),
            )(plainLong(number)),
          )

        case Cell.BLOCK_HASH(hash) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageCase.BlockDetail(
                    name = PageCase.Blocks().name,
                    url = s"block/${plainStr(hash)}",
                    pubs = List(
                      PubCase.BlockDetailPub(hash = plainStr(hash)),
                      PubCase.TxPub(
                        page = 1,
                        blockHash = plainStr(hash),
                        sizePerRequest = 10,
                      ),
                    ),
                  ),
                ),
              ),
              dataAttr("tooltip-text", plainStr(hash)),
            )(hash10(hash)),
          )
        case Cell.TX_HASH(hash) =>
          div(`class` := "cell type-3")(
            span(
              onClick(
                PageMsg.PreUpdate(
                  PageCase.TxDetail(
                    name = PageCase.Transactions().name,
                    url = s"transaction/${plainStr(hash)}",
                    pubs = List(PubCase.TxDetailPub(hash = plainStr(hash))),
                  ),
                ),
              ),
            )(
              plainStr(hash),
            ),
          )
        case Cell.TX_HASH10(hash) =>
          div(`class` := "cell type-3")(
            span(
              dataAttr("tooltip-text", plainStr(hash)),
              onClick(
                PageMsg.PreUpdate(
                  PageCase.TxDetail(
                    name = PageCase.Transactions().name,
                    url = s"transaction/${plainStr(hash)}",
                    pubs = List(PubCase.TxDetailPub(hash = plainStr(hash))),
                  ),
                ),
              ),
            )(
              hash10(hash),
            ),
          )

        case _ => div(`class` := "cell")(span()("NO - CELL")),
    )
    .toList
