package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
// import java.nio.file.{Files, Paths, StandardOpenOption}
// import io.circe.{Decoder, Encoder}

object UnderTxMsg:
  private val onResponse: Response => Msg = response =>
    Log.log("json")
    val json = Log.log(response.body)

    val parsed = parse(json) match
      case Right(r) => Right(r)
      case Left(l)  => Left(l.message)

    Log.log("parsed")
    Log.log(parsed)

    val deserialised =
      parsed.flatMap { json =>
        json.hcursor
          // .downField("data")
          .get[String]("message")
          .toOption
          .toRight("wrong json format")
      }

    Log.log("deserialised")
    Log.log(deserialised)

    deserialised match
      case Left(e)  => TxMsg.GetError(e)
      case Right(r) => TxMsg.GetNewTx(r)

  private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    Decoder[Msg](onResponse, onError)

object OnTxMsg:
  val input_file = "tx_list.json"
  // val path       = Paths.get(input_file)

  def getTxList(topic: String): Cmd[IO, Msg] =
    Log.log("getTxList")
    // Log.log(path)
    val url =
      s"http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10"
      // s"https://dog.ceo/api/breeds/image/random"
    Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)

  def sampleTxList() = ""
