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
    // 1. json:
    // String => Json => Decode[TxList]

    val json = log(response.body) // String
    val parsed = log(parse(json) match // Json
      case Right(r) => Right(r)
      case Left(l)  => Left(l.message),
    )
    // val parsed_TxList = log("parsed_TxList")
    // val parsed_TxList = decode[TxList](json)

    val deserialised =
      log(parsed.flatMap { json =>
        json.hcursor
          .get[String]("msg")
          .toOption
          .toRight("wrong json format")
      })

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
