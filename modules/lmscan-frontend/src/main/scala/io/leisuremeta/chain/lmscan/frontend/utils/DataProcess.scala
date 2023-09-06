package io.leisuremeta.chain.lmscan
package frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.*
import tyrian.http.*
import scala.scalajs.js
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import common.model._

object Parse:
  import io.circe.*, io.circe.generic.semiauto.*
  given Decoder[BlockModel] = deriveDecoder[BlockModel]
  given Decoder[TxModel] = deriveDecoder[TxModel]
  given Decoder[NftInfoModel] = deriveDecoder[NftInfoModel]
  given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
  given Decoder[BlcList] = deriveDecoder[BlcList]
  given Decoder[TxList] = deriveDecoder[TxList]
  given Decoder[NftList] = deriveDecoder[NftList]
  given Decoder[NftTokenList] = deriveDecoder[NftTokenList]
  given Decoder[TxDetail] = deriveDecoder[TxDetail]
  given Decoder[TransferHist] = deriveDecoder[TransferHist]
  given Decoder[BlockDetail] = deriveDecoder[BlockDetail]
  given Decoder[AccountDetail] = deriveDecoder[AccountDetail]
  given Decoder[TxInfo] = deriveDecoder[TxInfo]
  given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
  given Decoder[SummaryChart] = deriveDecoder[SummaryChart]
  given Decoder[NftDetail] = deriveDecoder[NftDetail]
  given Decoder[NftFileModel] = deriveDecoder[NftFileModel]
  given Decoder[NftActivity] = deriveDecoder[NftActivity]
  given Decoder[NftSeasonModel] = deriveDecoder[NftSeasonModel]

  def responseHandler(model: ApiModel): Response => Msg = res=>
    res.status match
      case Status(400, _) => ErrorMsg
      case Status(500, _) => ErrorMsg
      case _ => onResponse(model)(res)
    
  def onResponse(model: ApiModel): Response => Msg = response =>
    (model, parse(response.body)) match
      case (_, Left(e)) => ErrorMsg
      case (_: TxModel, Right(json)) => UpdateModel(decode[TxList](response.body).getOrElse(TxList()))
      case (_: BlockModel, Right(json)) => UpdateModel(decode[BlcList](response.body).getOrElse(BlcList()))
      case (_: NftModel, Right(json)) => UpdateModel(decode[NftList](response.body).getOrElse(NftList()))
      case (_: NftTokenModel, Right(json)) => UpdateModel(decode[NftTokenList](response.body).getOrElse(NftTokenList()))
      case (_: TxDetail, Right(json)) => UpdateModel(decode[TxDetail](response.body).getOrElse(TxDetail()))
      case (_: BlockDetail, Right(json)) => UpdateModel(decode[BlockDetail](response.body).getOrElse(BlockDetail()))
      case (_: AccountDetail, Right(json)) => UpdateModel(decode[AccountDetail](response.body).getOrElse(AccountDetail()))
      case (_: NftDetail, Right(json)) => UpdateModel(decode[NftDetail](response.body).getOrElse(NftDetail()))
      case (_: SummaryModel, Right(json)) => UpdateModel(decode[SummaryModel](response.body).getOrElse(SummaryModel()))
      case (_: SummaryChart, Right(json)) => UpdateModel(decode[SummaryChart](response.body).getOrElse(SummaryChart()))
      case (_, Right(json)) => ErrorMsg

object DataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def onError(e: HttpError): Msg = ErrorMsg

  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blist(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def tlist(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def nlist(page: Int, size: Int) = s"${base}nft/list?pageNo=${page}&sizePerRequest=${size}"
  def ntlist(id: String, page: Int, size: Int) = s"${base}nft/$id?pageNo=${page}&sizePerRequest=${size}"
  def getData(model: TxModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = tlist, page = model.page - 1, size = model.size)).withTimeout(5.seconds),
      Decoder[Msg](Parse.responseHandler(model), onError)
    )
  def getData(model: BlockModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = blist, page = model.page - 1, size = model.size)).withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: NftModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = nlist, page = model.page - 1, size = model.size)).withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: NftTokenModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(ntlist(model.id, model.page - 1, model.size)).withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(detail: TxDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(detail: BlockDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}block/${detail.hash.getOrElse("")}/detail").withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(model: AccountDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}account/${model.address.getOrElse("")}/detail").withTimeout(5.seconds),
      Decoder[Msg](Parse.responseHandler(model), onError)
    )
  def getData(model: NftFileModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}nft/${model.tokenId.getOrElse("")}/detail").withTimeout(5.seconds),
      Decoder[Msg](Parse.onResponse(NftDetail()), onError)
    )
  def getData(model: SummaryModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/main"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: SummaryChart): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/chart"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def globalSearch(v: String) = 
    val msg = v.length match
      case 40 => AccountDetailPage(v)
      case 42 => AccountDetailPage(v)
      case 25 => NftDetailPage(v)
      case 64 => TxDetailPage(v)
      case _ => AccountDetailPage(v)
    Cmd.Emit(RouterMsg.NavigateTo(msg))
