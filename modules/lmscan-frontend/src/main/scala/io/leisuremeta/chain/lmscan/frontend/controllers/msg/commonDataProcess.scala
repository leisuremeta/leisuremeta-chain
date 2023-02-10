package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import scala.scalajs.js
import Dom.*

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(page: PageName): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        PageMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        page match
          case PageName.AccountDetail(_) =>
            Log.log(s"${page}: ${response.body}")
          case _ => ""

        PageMsg.DataUpdate(response.body, page)
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse(page: PageName): Decoder[Msg] =
    Decoder[Msg](onResponse(page), onError)

object OnDataProcess:
  val host = js.Dynamic.global.process.env.BACKEND_URL
  val port = js.Dynamic.global.process.env.BACKEND_PORT
  val base = s"http://${host}:${port}"

  def getData(
      pageName: PageName,
      payload: ApiPayload = ApiPayload("1"),
  ): Cmd[IO, Msg] =

    val url = pageName match
      case PageName.DashBoard =>
        s"${base}/summary/main"
      case PageName.Transactions =>
        s"${base}/tx/list?pageNo=${(payload.page.toInt - 1).toString()}&sizePerRequest=10"
      case PageName.Blocks =>
        s"${base}/block/list?pageNo=${(payload.page.toInt - 1).toString()}&sizePerRequest=10"
      case PageName.BlockDetail(hash) =>
        s"${base}/block/$hash/detail"
      case PageName.TransactionDetail(hash) =>
        s"${base}/tx/$hash/detail"
      case PageName.AccountDetail(hash) =>
        s"${base}/account/$hash/detail"
      case PageName.NftDetail(hash) =>
        s"${base}/nft/$hash/detail"

      case _ => s"${base}/summary/main"

    Http.send(Request.get(url), UnderDataProcess.fromHttpResponse(pageName))
