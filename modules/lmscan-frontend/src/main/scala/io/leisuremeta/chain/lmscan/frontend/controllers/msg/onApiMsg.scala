package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import scala.scalajs.js

object UnderApiMsg:
  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        ApiMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        ApiMsg.Update(response.body)
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnApiMsg:
  def getSummaryData: Cmd[IO, Msg] =
    val host = js.Dynamic.global.process.env.BACKEND_URL
    val port = js.Dynamic.global.process.env.BACKEND_PORT

    val url = s"http://${host}:${port}/summary/main"
    Http.send(Request.get(url), UnderApiMsg.fromHttpResponse)
