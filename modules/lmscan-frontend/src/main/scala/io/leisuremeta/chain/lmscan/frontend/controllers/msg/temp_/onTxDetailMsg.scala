// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import io.circe.parser.*
// import tyrian.Html.*
// import tyrian.*
// import tyrian.http.*
// import io.circe.syntax.*
// import scala.scalajs.js

// object UnderTxDetailMsg:
//   private val onResponse: Response => Msg = response =>
//     import io.circe.*, io.circe.generic.semiauto.*

//     val parseResult: Either[ParsingFailure, Json] = parse(response.body)
//     Log.log("parseResult??")
//     Log.log(parseResult)
//     parseResult match
//       case Left(parsingError) =>
//         TxDetailMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
//       case Right(json) => {
//         TxDetailMsg.Update(response.body)
//       }

//   private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

//   def fromHttpResponse: Decoder[Msg] =
//     Decoder[Msg](onResponse, onError)

// object OnTxDetailMsg:
//   def getTxDetail(hash: String): Cmd[IO, Msg] =
//     val host = js.Dynamic.global.process.env.BACKEND_URL
//     val port = js.Dynamic.global.process.env.BACKEND_PORT

//     val url = s"http://${host}:${port}/tx/$hash/detail"
//     Http.send(Request.get(url), UnderTxDetailMsg.fromHttpResponse)
