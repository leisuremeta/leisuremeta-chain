package io.leisuremeta.chain.lmscan.frontend

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

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(page: PageName): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        PageMsg.GetError(s"Invalid JSON object: ${parsingError.message}", page)
      case Right(json) => {
        page match
          case PageName.DashBoard =>
            ""
          case _ =>
            dom.document
              .querySelector("#loader-container")
              .asInstanceOf[HTMLElement]
              .style
              .display = "none"

        page match
          case PageName.AccountDetail(_) =>
            Log.log(s"${page}: ${response.body}")
          case _ => ""

        page match
          case PageName.Page64(hash) =>
            PageMsg.DataUpdate(response.body, PageName.TransactionDetail(hash))
          case _ => PageMsg.DataUpdate(Log.log(response.body), page)

      }

  private val onError: HttpError => Msg = e =>
    dom.document
      .querySelector("#loader-container")
      .asInstanceOf[HTMLElement]
      .style
      .display = "none"
    ApiMsg.GetError(e.toString)

  def fromHttpResponse(page: PageName): Decoder[Msg] =
    // dom.document.querySelector("#loader-container").asInstanceOf[HTMLElement].style.display = "none"
    Decoder[Msg](onResponse(page), onError)

object OnDataProcess:
  // val host = js.Dynamic.global.process.env.BACKEND_URL
  // val port = js.Dynamic.global.process.env.BACKEND_PORT

  var base = js.Dynamic.global.process.env.BASE_API_URL

  def getData(
      pageName: PageName,
      payload: ApiPayload = ApiPayload("1"),
  ): Cmd[IO, Msg] =
    dom.document
      .querySelector("#loader-container")
      .asInstanceOf[HTMLElement]
      .style
      .display = "block"

    val url = pageName match
      case PageName.DashBoard =>
        s"$base/summary/main"
      case PageName.Transactions =>
        s"$base/tx/list?pageNo=${(payload.page.toInt - 1).toString()}&sizePerRequest=10"
      case PageName.Blocks =>
        s"$base/block/list?pageNo=${(payload.page.toInt - 1).toString()}&sizePerRequest=10"
      case PageName.BlockDetail(hash) =>
        s"$base/block/$hash/detail"
      case PageName.TransactionDetail(hash) =>
        s"$base/tx/$hash/detail"
      case PageName.AccountDetail(hash) =>
        s"$base/account/$hash/detail"
      case PageName.NftDetail(hash) =>
        s"$base/nft/$hash/detail"
      case PageName.Page64(hash) =>
        Log.log(s"여기로 검색? $base/tx/$hash/detail")
        s"$base/tx/$hash/detail"

      case _ => s"$base/summary/main"

    Http.send(
      Request.get(url).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pageName),
    )
