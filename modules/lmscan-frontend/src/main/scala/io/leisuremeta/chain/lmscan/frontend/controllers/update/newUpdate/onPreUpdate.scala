// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import io.circe.parser.*
// import tyrian.Html.*
// import tyrian.*
// import tyrian.http.*
// import io.circe.syntax.*

// // object UnderTxMsg:
// //   private val onResponse: Response => Msg = response =>
// //     import io.circe.*, io.circe.generic.semiauto.*
// //     val parseResult: Either[ParsingFailure, Json] = parse(response.body)
// //     parseResult match
// //       case Left(parsingError) =>
// //         TxMsg.GetError(s"Invalid JSON object: ${parsingError.message}")
// //       case Right(json) => {
// //         TxMsg.GetNewTx(response.body)
// //       }

// //   private val onError: HttpError => Msg = e => ApiMsg.GetError(e.toString)

// //   def fromHttpResponse: Decoder[Msg] =
// //     Decoder[Msg](onResponse, onError)

// object OnPreUpdate:
//   def dataUpdateProcess(str: String): Cmd[IO, Msg] = (
//     model.copy(prevPage = model.curPage, searchValueStore = search),
//     // Cmd.Emit(PageMsg.ataUpdateProcess),
//     OnPreUpdate.dataUpdateProcess(search),
//   )
//   def effect(s: String)              = Log.log(s)
//   def parseSearch(search: String): Cmd[IO, Msg] =
//     val url =
//       s"http://localhost:8081/tx/list?pageNo=${(page.toInt - 1).toString()}&sizePerRequest=10"
//   Http.send(Request.get(url), UnderTxMsg.fromHttpResponse)
