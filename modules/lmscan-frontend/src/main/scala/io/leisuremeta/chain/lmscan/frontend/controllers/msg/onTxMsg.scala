package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*

object UnderTxMsg:
  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        // throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
        TxMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        TxMsg.GetNewTx(response.body)
        // val decoded = TxParser.decodeParser(response.body)
        // decoded match {
        //   case Right(r) => TxMsg.GetNewTx(r)
        //   case Left(e)  => TxMsg.GetError("Filed json decode")
        // }
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnTxMsg:
  def getTxList(topic: String): Cmd[IO, Msg] =
    val url =
      s"http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10"
    Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)
