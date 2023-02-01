package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*

object UnderTxDetailMsg:
  private val onResponse: Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        TxDetailMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        TxDetailMsg.Update(response.body)
      }

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnTxDetailMsg:
  def getTxDetail(hash: String): Cmd[IO, Msg] =
    val hash =
      "1513b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da"
    val url =
      s"http://localhost:8081/tx/$hash/detail"
    Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)
