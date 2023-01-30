package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
// import io.circe._
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import Log.*
// import io.circe.{Decoder, Encoder}

object UnderTxMsg:
  private val onResponse: Response => Msg = response =>

    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        throw new IllegalArgumentException(
          s"Invalid JSON object: ${parsingError.message}",
        )
      case Right(json) => {
        val decoded = TxParser.decodeParser(response.body)
        decoded match
          case Right(r) => r.payload.foreach { println }
          case Left(e)  => log(e)
      }

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
    val url =
      s"http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10"
    Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)
