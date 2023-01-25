package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*

object UnderApiMsg:
  private val onResponse: Response => Msg = response =>
    val json = Log.log(response.body)

    val parsed = parse(json) match
      case Right(r) => Right(r)
      case Left(l)  => Left(l.message)

    val deserialised =
      parsed.flatMap { json =>
        json.hcursor
          // .downField("data")
          .get[String]("message")
          .toOption
          .toRight("wrong json format")
      }

    Log.log(deserialised)

    deserialised match
      case Left(e)  => ApiMsg.GetError(e)
      case Right(r) => ApiMsg.GetNewGif(r)

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnApiMsg:

  def getRandomGif(topic: String): Cmd[IO, Msg] =
    Log.log(s"getRandomGif $topic")
    val url =
      s"https://dog.ceo/api/breeds/image/$topic"
    Http.send(Request.get(url), UnderApiMsg.fromHttpResponse)
