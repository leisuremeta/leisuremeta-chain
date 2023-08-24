package io.leisuremeta.chain.lmscan
package frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import scala.scalajs.js
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import common.model._

object Parse:
  import io.circe.*, io.circe.generic.semiauto.*
  given Decoder[BlockModel] = deriveDecoder[BlockModel]
  given Decoder[TxModel] = deriveDecoder[TxModel]
  given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
  given Decoder[BlcList] = deriveDecoder[BlcList]
  given Decoder[TxList] = deriveDecoder[TxList]
  given Decoder[TxDetail] = deriveDecoder[TxDetail]
  given Decoder[TransferHist] = deriveDecoder[TransferHist]
  given Decoder[BlockDetail] = deriveDecoder[BlockDetail]
  given Decoder[AccountDetail] = deriveDecoder[AccountDetail]
  given Decoder[TxInfo] = deriveDecoder[TxInfo]
  given Decoder[SummaryModel] = deriveDecoder[SummaryModel]

  def onResponse(model: TxModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        PageMsg.UpdateTx(decode[TxList](response.body).getOrElse(model.list))
  def onResponse(model: BlockModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        PageMsg.UpdateBlc(decode[BlcList](response.body).getOrElse(model.list))

  def onResponse(model: TxDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        PageMsg.UpdateTxDetail(decode[TxDetail](response.body).getOrElse(model))
  def onResponse(model: BlockDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        PageMsg.UpdateBlcDetail(decode[BlockDetail](response.body).getOrElse(model))
  def onResponse(model: AccountDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        PageMsg.UpdateAccDetail(decode[AccountDetail](response.body).getOrElse(model))

  def onResponse(model: SummaryModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) => PageMsg.Update1(decode[SummaryModel](response.body).getOrElse(SummaryModel()))

object DataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  private val onError: HttpError => Msg = e => ErrorMsg
  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blist(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def tlist(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def getData(model: TxModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = tlist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: BlockModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = blist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(detail: TxDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(detail: BlockDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}block/${detail.hash.getOrElse("")}/detail").withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(detail: AccountDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}account/${detail.address.getOrElse("")}/detail").withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(model: SummaryModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/main"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )

    // Http.send(
    //   Request.get(get_api_link(pub, model)).withTimeout(30.seconds),
    //   UnderDataProcess.fromHttpResponse(pub),
    // )
      // case PubCase.BoardPub(page, _, _) =>
      //   s"$base/summary/main"

      // case PubCase.BlockPub(page, sizePerRequest, _, _) =>
      //   s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=${sizePerRequest}"

      // case PubCase.TxPub(
      //       page,
      //       sizePerRequest,
      //       accountAddr,
      //       blockHash,
      //       subtype,
      //       _,
      //       _,
      //     ) =>
      //   s"$base/tx/list?pageNo=${(page - 1)
      //       .toString()}&sizePerRequest=${sizePerRequest}" ++ {
      //     accountAddr match
      //       case "" => ""
      //       case _  => s"&accountAddr=${accountAddr}"
      //   } ++ {
      //     blockHash match
      //       case "" => ""
      //       case _  => s"&blockHash=${blockHash}"
      //   } ++ {
      //     subtype match
      //       case "" => ""
      //       case _  => s"&subtype=${subtype}"
      //   }

      // case PubCase.BlockDetailPub(hash, _, _) =>
      //   s"$base/block/$hash/detail"

      // case PubCase.TxDetailPub(hash, _, _) =>
      //   s"$base/tx/$hash/detail"

      // case PubCase.NftDetailPub(hash, _, _) =>
      //   s"$base/nft/$hash/detail"

      // case PubCase.AccountDetailPub(hash, _, _) =>
      //   s"$base/account/$hash/detail"
// def getPageFromString(search: String): PageCase | CommandCaseMode |
  //   CommandCaseLink =
  //   search match
  //     case ":on"    => CommandCaseMode.Development
  //     case ":off"   => CommandCaseMode.Production
  //     case ":prod"  => CommandCaseLink.Production
  //     case ":dev"   => CommandCaseLink.Development
  //     case ":local" => CommandCaseLink.Local
  //     case subtype if subtypeList.contains(subtype.replaceAll(" ", "")) =>
  //       Transactions()
  //     case _ =>
  //       search.length() match
  //         case 40 =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )
  //         case 42 =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )
  //         case 25 =>
  //           NftDetail(
  //             url = s"nft/${search}",
  //           )
  //         case 64 =>
  //           TxDetail(
  //             name = Transactions().name,
  //             url = s"transaction/${search}",
  //             pubs = List(PubCase.TxDetailPub(hash = search)),
  //           )
  //         case _ =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )

  //     case s"/nft/${hash}" if hash.length() == 25 =>
  //       NftDetail(
  //         url = s"nft/${hash}",
  //       )

  //     case _ => DashBoard()
