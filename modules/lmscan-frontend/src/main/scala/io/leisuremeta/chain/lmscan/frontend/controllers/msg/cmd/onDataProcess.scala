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
import io.leisuremeta.chain.lmscan.frontend.Log.log

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(page: PageCase): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        // PageMsg.GetError(s"Invalid JSON object: ${parsingError.message}", page)
        PageMsg.BackObserver
      case Right(json) => {
        PageMsg.DataUpdate(response.body)
        // page match
        //   case PageCase.DashBoard =>
        //     ""
        //   case _ =>
        //     dom.document
        //       .querySelector("#loader-container")
        //       .asInstanceOf[HTMLElement]
        //       .style
        //       .display = "none"

        // page match
        //   case PageCase.AccountDetail(_) =>
        //     log("#?")
        //     log(s"${page}: ${response.body}")
        //   case _ => ""

        // page match
        //   case PageCase.Page64(hash) =>
        //     PageMsg.DataUpdate(response.body, PageCase.TransactionDetail(hash))
        //   case _ => PageMsg.DataUpdate(log(response.body), page)

      }

  private val onError: HttpError => Msg = e =>
    // dom.document
    //   .querySelector("#loader-container")
    //   .asInstanceOf[HTMLElement]
    //   .style
    //   .display = "none"
    // ApiMsg.GetError(e.toString)
    PageMsg.BackObserver

  def fromHttpResponse(page: PageCase): Decoder[Msg] =
    // dom.document.querySelector("#loader-container").asInstanceOf[HTMLElement].style.display = "none"
    Decoder[Msg](onResponse(page), onError)

object OnDataProcess:
  // val host = js.Dynamic.global.process.env.BACKEND_URL
  // val port = js.Dynamic.global.process.env.BACKEND_PORT

  // !FIX
  // var base = js.Dynamic.global.process.env.BASE_API_URL_DEV
  var base = js.Dynamic.global.process.env.BASE_API_URL

  def getData(
      page: PageCase,
      // payload: ApiPayload = ApiPayload("1"),
  ): Cmd[IO, Msg] =
    // dom.document
    //   .querySelector("#loader-container")
    //   .asInstanceOf[HTMLElement]
    //   .style
    //   .display = "block"

    val url = page match
      case PageCase.DashBoard(_, _) =>
        s"$base/summary/main"
      case PageCase.Transactions(_, _) =>
        // s"$base/tx/list?pageNo=${(page - 1).toString()}&sizePerRequest=10"
        s"$base/tx/list?pageNo=${(0).toString()}&sizePerRequest=10"
      case PageCase.Blocks(_, _, _) =>
        // s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=10"
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"

      case _ =>
        s"$base/block/list?pageNo=${(0).toString()}&sizePerRequest=10"
      // s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=10"

      // case PageCase.BlockDetail(hash) =>
      //   s"$base/block/$hash/detail"
      // case PageCase.TransactionDetail(hash) =>
      //   s"$base/tx/$hash/detail"
      // case PageCase.AccountDetail(hash) =>
      //   s"$base/account/$hash/detail"
      // case PageCase.NftDetail(hash) =>
      //   s"$base/nft/$hash/detail"
      // case PageCase.Page64(hash) =>
      //   log(s"#page64 $base/tx/$hash/detail")
      //   s"$base/tx/$hash/detail"

      // case _ => s"$base/summary/main"

    Http.send(
      Request.get(url).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(page),
    )
