package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*

object UnderAccountDetailMsg:
  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        AccountDetailMsg.GetError(
          s"Invalid JSON object: ${parsingError.message}",
        )
      case Right(json) => {
        AccountDetailMsg.Update(response.body)
      }

  private val onError: HttpError => Msg = e =>
    AccountDetailMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnAccountDetailMsg:
  def getAcountDetail(hash: String): Cmd[IO, Msg] =
    val url = s"http://localhost:8081/account/${hash}/detail"
    Http.send(Request.get(url), UnderAccountDetailMsg.fromHttpResponse)
