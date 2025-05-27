package io.leisuremeta.chain.lmscan.agent.service

import sttp.client3.*
import cats.effect.kernel.Async
import io.circe.Decoder
import io.circe.parser.decode
import cats.implicits.toFunctorOps
import cats.data.EitherT

trait RequestServiceApp[F[_]: Async]:
  def getResult[A](url: String)(using Decoder[A]): EitherT[F, String, A]
  def getResultWithJsonString[A](url: String)(using
      Decoder[A],
  ): EitherT[F, String, (A, String)]
  def getResultFromKeyApi[A](url: String, hs: Map[String, String])(using
      Decoder[A],
  ): EitherT[F, String, A]

object RequestService:
  def parseRes(
      res: Response[Either[String, String]],
  ): Either[String, String] = res.body

  def parseJson[A](json: String)(using Decoder[A]): Either[String, A] =
    decode(json) match
      case Left(v)  => Left(v.toString)
      case Right(v) => Right(v)

  def build[F[_]: Async](backend: SttpBackend[F, Any]) =
    new RequestServiceApp[F]:
      def getResult[A](url: String)(using Decoder[A]) =
        getResultWithJsonString(url).fmap: v =>
          v._1

      def getResultWithJsonString[A](url: String)(using Decoder[A]) =
        EitherT.apply:
          basicRequest
            .get(uri"$url")
            .send(backend)
            .map: res =>
              for
                body <- parseRes(res)
                json <- parseJson[A](body)
              yield (json, body)

      def getResultFromKeyApi[A](url: String, hs: Map[String, String])(using
          Decoder[A],
      ) =
        EitherT.apply:
          basicRequest
            .get(uri"$url")
            .headers(hs)
            .send(backend)
            .map: res =>
              for
                body <- parseRes(res)
                json <- parseJson[A](body)
              yield json
