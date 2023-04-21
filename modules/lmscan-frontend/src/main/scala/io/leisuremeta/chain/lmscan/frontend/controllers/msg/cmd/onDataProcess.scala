package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import io.circe.syntax.*
import scala.scalajs.js
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.common.model.PageResponse
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*

case class ApiPayload(page: String)

object UnderDataProcess:

  private def onResponse(pub: PubCase): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    val parseResult: Either[ParsingFailure, Json] = parse(response.body)
    parseResult match
      case Left(parsingError) =>
        PageMsg.DeleteObserver
      case Right(json) =>
        PageMsg.DataUpdate(
          update_PubCase_data(pub, response.body),
        )

  private val onError: HttpError => Msg = e => PageMsg.BackObserver

  def fromHttpResponse(pub: PubCase): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)

object OnDataProcess:

  def getData(
      pub: PubCase,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(get_api_link(pub)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
