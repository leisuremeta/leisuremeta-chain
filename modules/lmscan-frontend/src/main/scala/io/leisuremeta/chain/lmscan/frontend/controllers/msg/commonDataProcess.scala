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

  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        PageMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        PageMsg.DataUpdate(response.body)
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnDataProcess:
  val host = js.Dynamic.global.process.env.BACKEND_URL
  val port = js.Dynamic.global.process.env.BACKEND_PORT

  def getData(
      pageName: PageName,
      payload: ApiPayload = ApiPayload("1"),
  ): Cmd[IO, Msg] =

    val url = pageName match
      case PageName.DashBoard =>
        s"http://${host}:${port}/summary/main"
      case PageName.Transactions =>
        s"http://${host}:${port}/tx/list?pageNo=${(payload.page.toInt - 1).toString()}&sizePerRequest=10"

      case _ => s"http://${host}:${port}/summary/main"

    Http.send(Request.get(url), UnderDataProcess.fromHttpResponse)
