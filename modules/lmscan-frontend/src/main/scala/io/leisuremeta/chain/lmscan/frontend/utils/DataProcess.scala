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

  def onResponse(model: TxModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[TxList](response.body).getOrElse(TxList()))
  def onResponse(model: BlockModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[BlcList](response.body).getOrElse(BlcList()))
  def onResponse(model: NftModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[NftList](response.body).getOrElse(NftList()))
  def onResponse(model: NftTokenModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[NftTokenList](response.body).getOrElse(NftTokenList()))

  def onResponse(model: TxDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[TxDetail](response.body).getOrElse(model))
  def onResponse(model: BlockDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[BlockDetail](response.body).getOrElse(model))
  def onResponse(model: AccountDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[AccountDetail](response.body).getOrElse(model))
  def onResponse(model: NftDetail): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) =>
        UpdateModel(decode[NftDetail](response.body).getOrElse(model))

  def onResponse(model: SummaryModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) => UpdateModel(decode[SummaryModel](response.body).getOrElse(SummaryModel()))
  def onResponse(model: SummaryChart): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => ErrorMsg
      case Right(json) => UpdateModel(decode[SummaryChart](response.body).getOrElse(SummaryChart()))

object DataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  private val onError: HttpError => Msg = e => ErrorMsg
  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blist(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def tlist(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def nlist(page: Int, size: Int) = s"${base}nft/list?pageNo=${page}&sizePerRequest=${size}"
  def ntlist(id: String, page: Int, size: Int) = s"${base}nft/$id?pageNo=${page}&sizePerRequest=${size}"
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
  def getData(model: NftModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = nlist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      Decoder[Msg](Parse.onResponse(model), onError)
    )
  def getData(model: NftTokenModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(ntlist(model.id, model.page - 1, model.size)).withTimeout(30.seconds),
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
  def getData(model: NftFileModel): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}nft/${model.tokenId.getOrElse("")}/detail").withTimeout(30.seconds),
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

  //     case s"/nft/${hash}" if hash.length() == 25 =>
  //       NftDetail(
  //         url = s"nft/${hash}",
  //       )

  //     case _ => DashBoard()
