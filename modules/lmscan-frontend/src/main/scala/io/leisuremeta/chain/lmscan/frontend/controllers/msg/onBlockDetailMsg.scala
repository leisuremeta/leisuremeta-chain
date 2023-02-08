package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import scala.scalajs.js

object UnderBlockDetailMsg:
  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        BlockDetailMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        BlockDetailMsg.Update(response.body)
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnBlockDetailMsg:
  def getBlockDetail(hash: String): Cmd[IO, Msg] =
    val host = js.Dynamic.global.process.env.BACKEND_URL
    val port = js.Dynamic.global.process.env.BACKEND_PORT

    // TODO :: url 관리 컴파일타임에 url 가져오기
    val url = s"http://${host}:${port}/block/$hash/detail"
    Http.send(Request.get(url), UnderBlockDetailMsg.fromHttpResponse)
