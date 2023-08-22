package io.leisuremeta.chain.lmscan
package frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import scala.scalajs.js
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*
import io.circe.syntax.*
import io.circe.parser.*
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.common.model.TxInfo
import common.model._

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(pub: PubCase): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    parse(response.body) match
      case Left(parsingError) =>
        // pub match
        //   case PubCase.TxDetailPub(hash, _, _) =>
        //     PageMsg.PreUpdate(
        //       BlockDetail(
        //         name = Blocks().name,
        //         url = s"block/${hash}",
        //         pubs = List(
        //           PubCase.BlockDetailPub(
        //             hash = hash,
        //           ),
        //           PubCase.TxPub(
        //             page = 1,
        //             blockHash = hash,
        //             sizePerRequest = 10,
        //           ),
        //         ),
        //       ),
        //     )
        //   case _ => PageMsg.RolloBack
        PageMsg.RolloBack

      case Right(json) =>
        PageMsg.DataUpdate(
          update_PubCase_data(pub, response.body),
        )

  private def onResponse(pub: BlocksModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.Update(decode[BlcList](response.body).getOrElse(pub.blcList))

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

  def fromHttpResponse(pub: BlocksModel): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)

  def fromHttpResponse(pub: PubCase): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)

  def fromHttpResponse(pub: String): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)

object OnDataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blcList(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def txList(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def getData(
      blcPage: BlocksModel,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = blcList, page = blcPage.page - 1, size = blcPage.size)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(blcPage),
    )
  def getData(
      pub: String,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getApi(pub)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
  def getApi(target: String) =
    target match
      case "a" => s"${base}summary/main"
      case "b" => getList(blcList)
      case "c" => getList(txList)
      case _ => s""

  def getData(
      pub: PubCase,
      model: Model,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(get_api_link(pub, model)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
