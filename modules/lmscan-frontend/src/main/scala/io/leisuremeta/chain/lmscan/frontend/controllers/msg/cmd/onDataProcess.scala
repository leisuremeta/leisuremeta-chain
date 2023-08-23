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
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*
import common.model._

case class ApiPayload(page: String)

object UnderDataProcess:
  private def onResponse(model: TxModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateTx(decode[TxList](response.body).getOrElse(model.list))
  private def onResponse(model: BlockModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateBlc(decode[BlcList](response.body).getOrElse(model.list))

  private def onResponse(model: TxDetail): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    given Decoder[TxDetail] = deriveDecoder[TxDetail]
    given Decoder[TransferHist] = deriveDecoder[TransferHist]
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateTxDetail(decode[TxDetail](response.body).getOrElse(model))

  private def onResponse(pub: String): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        pub match
          case "a" => PageMsg.Update1(decode[SummaryModel](response.body).getOrElse(SummaryModel()))
          case "b" => PageMsg.Update2(decode[BlcList](response.body).getOrElse(BlcList()))
          case "c" => PageMsg.Update3(decode[TxList](response.body).getOrElse(TxList()))
          case "_" => PageMsg.RolloBack

  private val onError: HttpError => Msg = e => PageMsg.BackObserver

  def fromHttpResponse(model: TxModel): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)
  def fromHttpResponse(model: BlockModel): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)
  def fromHttpResponse(pub: String): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)
  def fromHttpResponse(model: TxDetail): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)

object OnDataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blist(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def tlist(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def getData(
      model: TxModel,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = tlist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(model),
    )
  def getData(
      model: BlockModel,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = blist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(model),
    )
  def getData(
      pub: String,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getApi(pub)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
  def getData(
      detail: TxDetail,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(detail),
    )
  def getApi(target: String) =
    target match
      case "a" => s"${base}summary/main"
      case "b" => getList(blist)
      case "c" => getList(tlist)
      case _ => s""

  def getData(
      pub: PubCase,
      model: Model,
  ): Cmd[IO, Msg] =
    Cmd.None
    // Http.send(
    //   Request.get(get_api_link(pub, model)).withTimeout(30.seconds),
    //   UnderDataProcess.fromHttpResponse(pub),
    // )
