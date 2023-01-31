package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.http.*
import tyrian.*

object Board:
  // val LM_Price     = "LM Price = 0.394 USDT"
  // val Block_Number = "Block Number 21,872,421"
  // val Transactions = "24h Transactions 3,572,245"
  // val Accounts     = "Total Accounts 194,142,552"

  val LM_Price     = "LM Price = 0.394 USDT"
  val Block_Number = "Block Number 21,872,421"
  val Transactions = "24h Transactions 3,572,245"
  val Accounts     = "Total Accounts 194,142,552"

  val Key = "f33f816a-515a-44da-80cc-ca07be59eef6"
  val Url = "https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest"
  val CoinID = "20315"
  val headers = List(
    Header("Content-Type", "application/json; utf-8"),
    Header("Accept", "application/json"),
    Header("X-CMC_PRO_API_KEY", Key)
    )

  private val onResponse: Response => Msg = response =>
    Log.log("DashboardMsg.444444")
    import io.circe._, io.circe.generic.semiauto._
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)  
    parseResult match {
      case Left(parsingError) =>
        throw new IllegalArgumentException(s"Invalid JSON object: ${parsingError.message}")
      case Right(json) => {
        val json: Json = parse(response.body).getOrElse(null)
        val jsonAsMap = json.as[Map[String, String]]
        jsonAsMap match {
          case Right(r) => {
            // Log.log(r)
            DashboardMsg.GetNew(r.toString)
          }
          case Left(e)  => DashboardMsg.GetError("Filed json decode")
        }
      }
    }

  private val onError: HttpError => Msg = e => 
    DashboardMsg.GetError(e.toString)

  def fromHttpResponse: Decoder[Msg] =
    // Log.log("DashboardMsg.333333")
    Decoder[Msg](onResponse, onError)

  def update(model: Model): DashboardMsg => (Model, Cmd[IO, Msg]) =
    case DashboardMsg.GetNew(r) =>  
      // Log.log(r)
      (model, Cmd.None)
    case DashboardMsg.GetError(_) =>   
      Log.log("DashboardMsg.GetError")
      (model, Cmd.None)

  // Http.send(Request(method = Method.Get, url = Url).addHeaders(headers), fromHttpResponse)

object BoardView:
  def getTxList(topic: String): Cmd[IO, Msg] = Http.send(Request.get("https://pro-api.coinmarketcap.com/v1/cryptocurrency/quotes/latest"), Board.fromHttpResponse)

  def view(model: Model): Html[Msg] =
    Log.log("DashboardMsg.GetError111111")
    getTxList("hi")
    Log.log("DashboardMsg.GetError2222222")
    div(`class` := "board-area")(
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.LM_Price),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Block_Number),
        ),
      ),
      div(`class` := "board-list x")(
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Transactions),
        ),
        div(`class` := "board-container xy-center")(
          div(
            `class` := "board-text xy-center",
          )(Board.Accounts),
        ),
      ),
    )
