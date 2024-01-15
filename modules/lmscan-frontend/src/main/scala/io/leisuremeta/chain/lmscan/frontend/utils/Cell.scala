package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import scala.util.matching.Regex
import io.leisuremeta.chain.lmscan.common.model.*
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time._

def toDT(t: Int): String = LocalDateTime
  .ofEpochSecond(t, 0, ZoneOffset.UTC)
  .toString
  .replace("T", " ")

def timeAgo(t: Int): String =
  val now = LocalDateTime.now(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(0))).toEpochSecond(ZoneOffset.UTC)
  val timeGap = now - t
  List(
      ((timeGap / 31536000).toInt, " year ago"),
      ((timeGap / 2592000).toInt, " month ago"),
      ((timeGap / 86400).toInt, " day ago"),
      ((timeGap / 3600).toInt, " hour ago"),
      ((timeGap / 60).toInt, " min ago"),
      ((timeGap / 1).toInt, "s ago"),
    )
    .find((time, _) => time > 0)
    .map:
      case (time, msg) if time > 1 => time.toString + msg.replace(" a", "s a").replace("ss", "s")
      case (time, msg) => time.toString + msg
    .get

enum Cell:
  case ImageS(data: Option[String])              extends Cell
  case Image(data: Option[String])              extends Cell
  case Head(data: String, css: String = "cell") extends Cell
  case Any(data: String, css: String = "cell")  extends Cell
  case PriceS(
      price: Option[BigDecimal],
      css: String = "cell",
  )                                                             extends Cell
  case Price(
      price: Option[Double],
      balance: Option[BigDecimal],
      css: String = "cell",
  )                                                             extends Cell
  case Balance(data: Option[BigDecimal], css: String = "cell")  extends Cell
  case AGE(data: Option[Long])                                  extends Cell
  case DateS(data: Option[Instant], css: String = "cell")           extends Cell
  case DATE(data: Option[Long], css: String = "cell")           extends Cell
  case BLOCK_NUMBER(data: (Option[String], Option[Long]))       extends Cell
  case NftToken(data: NftInfoModel)                         extends Cell
  case NftDetail(data: NftSeasonModel, s: Option[String])                         extends Cell
  case BLOCK_HASH(data: Option[String])                         extends Cell
  case ACCOUNT_HASH(data: Option[String], css: String = "cell") extends Cell
  case TX_HASH(data: Option[String])                            extends Cell
  case Tx_VALUE(data: (Option[String], Option[String]))         extends Cell
  case PlainInt(data: Option[Int])                              extends Cell
  case PlainLong(data: Option[Long])                            extends Cell
  case PlainStr(
      data: Option[String] | Option[Int] | Option[Long],
      css: String = "cell",
  )                      extends Cell
  case AAA(data: String) extends Cell

object gen:
  def cell(cells: Cell*) = cells
    .map(_ match
      case Cell.ImageS(nftUri) =>
        img(
          `class` := "thumb-img",
          src     := s"${getOptionValue(nftUri, "-").toString}",
        )
      case Cell.Image(nftUri) =>
        List("mp3", "mp4")
          .find(data => plainStr(nftUri).contains(data)) match
          case Some(_) => // 비디오 포맷
            video(
              `class` := "nft-image p-10px",
              autoPlay,
              loop,
              name := "media",
            )(
              source(
                src    := s"${getOptionValue(nftUri, "-").toString}",
                `type` := "video/mp4",
              ),
            )
          case None => // 이미지 포맷
            img(
              `class` := "nft-image p-10px",
              src     := s"${getOptionValue(nftUri, "-").toString}",
            )

      case Cell.Head(data, css) => div(`class` := s"$css")(span()(data))
      case Cell.Any(data, css)  => div(`class` := s"$css")(span()(data))
      case Cell.PriceS(price, css) =>
        div(`class` := s"$css")(
          span(
            price match
              case Some(p) =>
                "$ " + DecimalFormat("#,###.####").format(p)
              case _ => "$ 0",
          ),
        )
      case Cell.Price(price, data, css) =>
        div(`class` := s"$css")(
          span(
            (price, data) match
              case (Some(p), Some(v)) =>
                val a = v / BigDecimal("1E+18") * BigDecimal(p)
                "$ " + DecimalFormat("#,###.####").format(a)
              case _ => "$ 0",
          ),
        )
      case Cell.Balance(data, css) =>
        div(`class` := s"$css")(
          span(
            data match
              case None => "- LM"
              case Some(v) =>
                val a = v / BigDecimal("1E+18")
                DecimalFormat("#,###.####").format(a) + " LM",
          ),
        )
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
                  ToPage(NftDetailModel(nftDetail = NftDetail(nftFile = Some(NftFileModel(tokenId = value)))))
                )
              case _ => EmptyAttribute,
          )(
            plainStr(subType).contains("Nft") match
              case true => plainStr(value)
              case _ =>
                value
                  .map(s => s.forall(Character.isDigit))
                  .getOrElse(false) match
                  case true =>
                    txValue(value)
                  case false => plainStr(value),
          ),
        )
      case Cell.ACCOUNT_HASH(hash, css) =>
        div(`class` := s"cell type-3 $css")(
          span(
            onClick(
              ToPage(AccDetailModel(accDetail = AccountDetail(address = hash))),
            ),
          )(
            accountHash(hash),
          ),
        )
      case Cell.AGE(data) =>
        div(`class` := "cell",
            dataAttr(
              "tooltip-text",
              toDT(plainLong(data).toInt),
            ),
          )(
            {
              val age = timeAgo(
                plainLong(data).toInt,
              )
              age match
                case x if x.contains("53 years") => "-"
                case _                           => age
            },
          )

      case Cell.DateS(data, css) =>
        data match
          case None => div()
          case Some(v) => 
            div(
              `class` := s"$css",
              dataAttr(
                "tooltip-text",
                v.toString
              )
            )(
              DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm (O)")
                .withZone(ZoneId.of("+09:00")) 
                .format(v)
            )
      case Cell.DATE(data, css) =>
        div(`class` := s"$css")(
            {
              val date = toDT(
                plainLong(data).toInt,
              ) + " +UTC"
              date match
                case x if x.contains("1970") => "-"
                case _                       => date

            },
        )

      case Cell.BLOCK_NUMBER((hash, number)) =>
        div(`class` := "cell type-3",
            onClick(
              ToPage(BlcDetailModel(blcDetail = BlockDetail(hash = hash))),
            ),
          )(plainLong(number))

      case Cell.NftToken(nftInfo) =>
        div(
          `class` := "cell",
          onClick(
            ToPage(NftTokenModel(id = nftInfo.season.get)),
          ),
        )(
          span(`class` := "season-nm")(plainSeason(nftInfo.season)),
          span(`class` := "type-3")(plainStr(nftInfo.seasonName)),
        )
      case Cell.NftDetail(nftInfo, s) =>
        div(
          `class` := "cell type-3",
          onClick(
            ToPage(NftDetailModel(nftDetail = NftDetail(nftFile = Some(NftFileModel(tokenId = nftInfo.tokenId)))))
          ),
        )(plainStr(s))
      case Cell.BLOCK_HASH(hash) =>
        div(
          `class` := "cell type-3",
          onClick(
            ToPage(BlcDetailModel(blcDetail = BlockDetail(hash = hash))),
          ),
        )(plainStr(hash))
      case Cell.TX_HASH(hash) =>
        div(
          `class` := "cell type-3",
          onClick(
            ToPage(TxDetailModel(txDetail = TxDetail(hash = hash)))
          ),
        )(
          plainStr(hash),
        )

      case _ => div(`class` := "cell")(span()),
    )
    .toList
