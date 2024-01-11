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
  given Decoder[SummaryBoard] = deriveDecoder[SummaryBoard]
  given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
  given Decoder[SummaryChart] = deriveDecoder[SummaryChart]
  given Decoder[NftInfoModel] = deriveDecoder[NftInfoModel]
  given Decoder[BlockInfo] = deriveDecoder[BlockInfo]
  given Decoder[TxDetail] = deriveDecoder[TxDetail]
  given Decoder[TransferHist] = deriveDecoder[TransferHist]
  given Decoder[BlockDetail] = deriveDecoder[BlockDetail]
  given Decoder[AccountDetail] = deriveDecoder[AccountDetail]
  given Decoder[AccountInfo] = deriveDecoder[AccountInfo]
  given Decoder[TxInfo] = deriveDecoder[TxInfo]
  given Decoder[NftDetail] = deriveDecoder[NftDetail]
  given Decoder[NftFileModel] = deriveDecoder[NftFileModel]
  given Decoder[NftActivity] = deriveDecoder[NftActivity]
  given Decoder[NftSeasonModel] = deriveDecoder[NftSeasonModel]
  given Decoder[BlockModel] = deriveDecoder[BlockModel]
  given Decoder[TxModel] = deriveDecoder[TxModel]
  given Decoder[BlcList] = deriveDecoder[BlcList]
  given Decoder[AccList] = deriveDecoder[AccList]
  given Decoder[TxList] = deriveDecoder[TxList]
  given Decoder[NftList] = deriveDecoder[NftList]
  given Decoder[NftTokenList] = deriveDecoder[NftTokenList]
  

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
      case (_: AccModel, Right(json)) => UpdateModel(decode[AccList](response.body).getOrElse(AccList()))
      case (_: NftModel, Right(json)) => UpdateModel(decode[NftList](response.body).getOrElse(NftList()))
      case (_: NftTokenModel, Right(json)) => UpdateModel(decode[NftTokenList](response.body).getOrElse(NftTokenList()))
      case (_: TxDetail, Right(json)) => UpdateModel(decode[TxDetail](response.body).getOrElse(TxDetail()))
      case (_: BlockDetail, Right(json)) => UpdateModel(decode[BlockDetail](response.body).getOrElse(BlockDetail()))
      case (_: AccountDetail, Right(json)) => UpdateModel(decode[AccountDetail](response.body).getOrElse(AccountDetail()))
      case (_: NftDetail, Right(json)) => UpdateModel(decode[NftDetail](response.body).getOrElse(NftDetail()))
      case (_: SummaryBoard, Right(json)) => UpdateModel(decode[SummaryBoard](response.body).getOrElse(SummaryBoard()))
      case (_: SummaryChart, Right(json)) => UpdateModel(decode[SummaryChart](response.body).getOrElse(SummaryChart()))
      case (_, Right(json)) => ErrorMsg

object DataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def onError(e: HttpError): Msg = ErrorMsg

  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def getData[T](model: ListPage[T]): Cmd[IO, Msg] =
    val url = model match
      case _: BlockModel => s"${base}block/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: TxModel => s"${base}tx/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: NftModel => s"${base}nft/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case m: NftTokenModel => s"${base}nft/${m.id}?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
      case _: AccModel => s"${base}account/list?pageNo=${model.page - 1}&sizePerRequest=${model.size}"
    Http.send(
      Request.get(url).withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(detail: TxDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(detail: BlockDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}block/${detail.hash.getOrElse("")}/detail").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(detail), onError)
    )
  def getData(model: AccountDetail): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}account/${model.address.getOrElse("")}/detail?p=1").withTimeout(10.seconds),
      Decoder[Msg](Parse.responseHandler(model), onError)
    )
  def getData(model: NftFileModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}nft/${model.tokenId.getOrElse("")}/detail").withTimeout(10.seconds),
      Decoder[Msg](Parse.onResponse(NftDetail()), onError)
    )
  def getData(model: SummaryBoard): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/main"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getDataAll(model: SummaryChart): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/chart/balance"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: SummaryChart): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}summary/chart/tx"),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def globalSearch(v: String) = 
    val msg = v.length match
      case 25 => NftDetailModel(nftDetail = NftDetail(nftFile = Some(NftFileModel(tokenId = Some(v)))))
      case 64 => TxDetailModel(txDetail = TxDetail(hash = Some(v)))
      case _ => AccDetailModel(accDetail = AccountDetail(address = Some(v)))
    Cmd.Emit(RouterMsg.ToDetail(msg))
