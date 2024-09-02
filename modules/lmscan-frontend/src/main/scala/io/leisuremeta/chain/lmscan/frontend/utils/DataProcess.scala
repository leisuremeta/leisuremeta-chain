package io.leisuremeta.chain.lmscan
package frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.*
import tyrian.http.*
import scala.scalajs.js
import scala.concurrent.duration.*
import common.model._
import tyrian.cmds.LocalStorage

object Parse:
  import io.circe.*, io.circe.generic.semiauto.*
  given Decoder[SummaryBoard] = deriveDecoder[SummaryBoard]
  given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
  given Decoder[SummaryChart] = deriveDecoder[SummaryChart]
  given Decoder[NftInfoModel] = deriveDecoder[NftInfoModel]
  given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
  given Decoder[AccountInfo] = deriveDecoder[AccountInfo]
  given Decoder[NftSeasonModel] = deriveDecoder[NftSeasonModel]
  given a: Decoder[PageResponse[AccountInfo]] = deriveDecoder[PageResponse[AccountInfo]]
  given b: Decoder[PageResponse[BlockInfo]] = deriveDecoder[PageResponse[BlockInfo]]
  given c: Decoder[PageResponse[TxInfo]] = deriveDecoder[PageResponse[TxInfo]]
  given d: Decoder[PageResponse[NftInfoModel]] = deriveDecoder[PageResponse[NftInfoModel]]
  given e: Decoder[PageResponse[NftSeasonModel]] = deriveDecoder[PageResponse[NftSeasonModel]]
    
  def searchResponse(): Response => Msg = response =>
    response.status match
      case Status(400, _) => ErrorMsg
      case Status(500, _) => ErrorMsg
      case _ => searchResult(response)

  def searchResult(response: Response): Msg = decode[SearchResult](response.body) match
    case Right(SearchResult.acc(x)) => GlobalSearchResult(AccDetailModel().set(x))
    case Right(SearchResult.tx(x)) => GlobalSearchResult(TxDetailModel(txDetail = x))
    case Right(SearchResult.blc(x)) => GlobalSearchResult(BlcDetailModel().set(x))
    case Right(SearchResult.nft(x)) => GlobalSearchResult(NftDetailModel(nftDetail = x))
    case _ => ErrorMsg

  def onResponse(model: ApiModel): Response => Msg = response =>
    response.status match
      case Status(400, _) => ErrorMsg
      case Status(500, _) => ErrorMsg
      case _ => result(model, response)

  def result(model: ApiModel, response: Response): Msg = (model, response.body) match
      case (_: BlcModel, str) => UpdateBlcs(decode[PageResponse[BlockInfo]](str).getOrElse(PageResponse()))
      case (_: TxModel, str) => UpdateTxs(decode[PageResponse[TxInfo]](str).getOrElse(PageResponse()))
      case (_: AccModel, str) => UpdateListModel[AccountInfo](decode[PageResponse[AccountInfo]](str).getOrElse(PageResponse()))
      case (_: NftModel, str) => UpdateListModel(decode[PageResponse[NftInfoModel]](str).getOrElse(PageResponse()))
      case (_: NftTokenModel, str) => UpdateListModel(decode[PageResponse[NftSeasonModel]](str).getOrElse(PageResponse()))
      case (_: TxDetail, str) => UpdateModel(decode[TxDetail](str).getOrElse(TxDetail()))
      case (_: BlockDetail, str) => UpdateModel(decode[BlockDetail](str).getOrElse(BlockDetail()))
      case (_: AccountDetail, str) => UpdateModel(decode[AccountDetail](str).getOrElse(AccountDetail()))
      case (_: NftDetail, str) => UpdateModel(decode[NftDetail](str).getOrElse(NftDetail()))
      case (_: SummaryBoard, str) => SetLocal("board", str)
      case (_: SummaryChart, str) => SetLocal("chart", str)
      case (_, str) => ErrorMsg
      case (_, Left(json)) => ErrorMsg

  def parseFromString(k: String, s: String): Msg = k match
    case "board" => UpdateModel(decode[SummaryBoard](s).getOrElse(SummaryBoard()))
    case "chart" => UpdateChart(decode[SummaryChart](s).getOrElse(SummaryChart()))

object DataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def onError(e: HttpError): Msg = ErrorMsg
  def getData(model: PageModel): Cmd[IO, Msg] =
    val url = model match
      case _: BlcModel => s"${base}block/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: TxModel => s"${base}tx/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: NftModel => s"${base}nft/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case m: NftTokenModel => s"${base}nft/${m.id}?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: AccModel => s"${base}account/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
    Http.send(
      Request.get(url).withTimeout(20.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(detail: TxDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(model: BlcDetailModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}block/${model.blcDetail.hash.getOrElse("")}/detail?p=${model.page}").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(model.blcDetail), onError)
    )
  def getData(model: AccDetailModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}account/${model.accDetail.address.getOrElse("")}/detail?p=${model.page}").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(model.accDetail), onError)
    )
  def getData(model: NftFileModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}nft/${model.tokenId.getOrElse("")}/detail").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(NftDetail()), onError)
    )
  def getDataAll(key: String): Cmd[IO, Msg] = key match
    case "chart" =>
      Http.send(
        Request.get(s"${base}summary/chart/balance"),
        Decoder[Msg](Parse.onResponse(SummaryChart()), onError)
      )
    case "board" =>
      Http.send(
        Request.get(s"${base}summary/main"),
        Decoder[Msg](Parse.onResponse(SummaryBoard()), onError)
      )
  def globalSearch(v: String): Cmd[IO, Msg] = 
    Http.send(
      Request.get(s"${base}search/${v}"),
      Decoder[Msg](Parse.searchResponse(), onError)
    )

  def getLocal(key: String): Cmd[IO, Msg] =
    LocalStorage.getItem(key):
      case Right(LocalStorage.Result.Found(s)) => 
        val (t, d) = s.splitAt(13)
        if  js.Date.now() - t.toDouble < 60 * 10 * 1000 then Parse.parseFromString(key, d) 
        else GetDataFromApi(key)
      case Left(LocalStorage.Result.NotFound(_)) => GetDataFromApi(key)

  def setLocal(k: String, d: String): Cmd[IO, Msg] =
    val now = js.Date.now().toString
    LocalStorage.setItem(k, now + d):
      case LocalStorage.Result.Success => Parse.parseFromString(k, d)
      case _ => Parse.parseFromString(k, d) 
