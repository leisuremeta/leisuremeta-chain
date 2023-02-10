// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import io.circe.parser.*
// import tyrian.Html.*
// import tyrian.*
// import tyrian.http.*
// import io.circe.syntax.*
// import scala.scalajs.js

// object UnderBlockMsg:
//   private val onResponse: Response => Msg = response =>
//     import io.circe.*, io.circe.generic.semiauto.*
//     val parseResult: Either[ParsingFailure, Json] = parse(response.body)
//     parseResult match
//       case Left(parsingError) =>
//         BlockMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
//       case Right(json) => {
//         BlockMsg.GetNewBlock(response.body)
//       }

//   private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

//   def fromHttpResponse: Decoder[Msg] =
//     Decoder[Msg](onResponse, onError)

// object OnBlockMsg:
//   def getBlockList(page: String): Cmd[IO, Msg] =
//     val host = js.Dynamic.global.process.env.BACKEND_URL
//     val port = js.Dynamic.global.process.env.BACKEND_PORT

//     val url = s"http://${host}:${port}/block/list?pageNo=${(page.toInt - 1).toString()}&sizePerRequest=10"
//     Http.send(Request.get(url), UnderBlockMsg.fromHttpResponse)
