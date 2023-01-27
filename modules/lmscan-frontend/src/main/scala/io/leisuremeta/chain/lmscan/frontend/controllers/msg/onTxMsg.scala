package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import Log.*
// import io.circe.{Decoder, Encoder}

object UnderTxMsg:
  private val onResponse: Response => Msg = response =>
    log("json")
    val json = response.body

    // log(json)
    // log(json.asJson)
    // log(json.asJson.hcursor)
    // log(CustomDecoder.txDecoder(json.asJson.hcursor))

    val parsed = parse(json) match
      case Right(r) => Right(r)
      case Left(l)  => Left(l.message)

    log("parsed")

    val deserialised =
      parsed.flatMap { json =>
        // json.as[TxList]
        json.hcursor
          .get[String]("msg")
          // json.hcursor
          //   .downField("data")
          //   .get[String]("message")
          .toOption
          .toRight("wrong json format")
      }

    log("deserialised")
    log(deserialised)

    deserialised match
      case Left(e)  => TxMsg.GetError(e)
      case Right(r) => TxMsg.GetNewTx(r)

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnTxMsg:
  def getTxList(topic: String): Cmd[IO, Msg] =
    log("getTxList")
    val url =
      s"http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10"
    Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)
