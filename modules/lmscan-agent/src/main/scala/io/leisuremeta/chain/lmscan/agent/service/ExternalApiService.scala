package io.leisuremeta.chain.lmscan.agent.service

import sttp.client3.SttpBackend
import cats.data.EitherT
import cats.effect.Async
import io.leisuremeta.chain.lmscan.agent.model.{LmPrice, Data}
import io.leisuremeta.chain.lmscan.agent.service.SummaryService
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.refined.*
import io.circe.syntax.*
import sttp.model.Uri
import cats.implicits.*
import sttp.client3.*

object ExternalApiService:
  def requestCoinMarket[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Double] =
    val lmTokenId = 20315
    val res = for 
      res <- get[F, LmPrice](backend) {
        uri"https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest?id=$lmTokenId"
      }.leftMap { msg =>
        scribe.error(s"getLmPrice error msg: $msg")  
        msg
      }
      result <- if res.status.error_code == 0 then 
        scribe.info(s"getLmPrice response: $res")
        EitherT.pure(res.data("20315").quote.USD.price)
      else {
        scribe.error(s"coin market api returned response error (code: ${res.status.error_code}, message: ${res.status.error_message} )"); 
        EitherT.pure(0.0)
      }
    yield (result)

    var price = 0.0
    val _ = SummaryService.getLastSavedLmPrice[F].flatMap {
      case Some(value) => {
        price = value
        EitherT.pure(Some(value))
      }
      case None => EitherT.pure(Some(0.0))
    }
    res.recover{(msg: String) => scribe.info(s"get error"); price}

  def getLmPrice[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Double] =
    for 
      result <- requestCoinMarket(backend)
    yield result

  def get[F[_]: Async, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, A] =
    EitherT {
      basicRequest
        .header("X-CMC_PRO_API_KEY", "aa17895d-2df9-4bf6-bb32-2943cd775c5d", replaceExisting = true)
        .get(uri)
        .send(backend)
        .map { response =>
          for
            body <- response.body
            _ <- Either.right(scribe.info(s"body: $body"))
            a    <- decode[A](body).leftMap(_.getMessage())
          yield a
        }
    }    
