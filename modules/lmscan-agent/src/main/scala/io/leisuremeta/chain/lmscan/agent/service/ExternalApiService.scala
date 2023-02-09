package io.leisuremeta.chain.lmscan.agent.service

import sttp.client3.SttpBackend
import cats.data.EitherT
import cats.effect.Async
import io.leisuremeta.chain.lmscan.agent.model.{LmPrice, Data}
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.refined.*
import io.circe.syntax.*
import sttp.model.Uri
import cats.implicits.*
import sttp.client3.*

object ExternalApiService:
  def requestCoinMarket[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, LmPrice] =
    val lmTokenId = 20315
    get[F, LmPrice](backend) {
      uri"https://pro-api.coinmarketcap.com/v2/cryptocurrency/quotes/latest?id=$lmTokenId"
    }.leftMap { msg =>
      scribe.error(s"getLmPrice error msg: $msg")  
      msg
    }

  def getLmPrice[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Option[Data]] =
    for 
      res <- requestCoinMarket(backend)
      _ <- EitherT.pure(scribe.info(s"coinMarket: $res"))
      result <- if res.status.error_code == 0 then 
        scribe.info(s"getLmPrice response: $res")
        EitherT.pure(Some(res.data.`21315`))
      else {
        scribe.error(s"coin market api returned response error (code: ${res.status.error_code}, message: ${res.status.error_message} )"); 
        EitherT.pure(None)
      }
    yield result

  def get[F[_]: Async, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, A] =
    EitherT {
      basicRequest
        .header("X-CMC_PRO_API_KEY", "b78d5940-b63d-4d90-99fc-58f65be72fa0", replaceExisting = true)
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
