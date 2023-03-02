package io.leisuremeta.chain.lmscan.backend.repository
import scala.io.Source
import cats.effect.{Async}
import cats.data.EitherT
import com.typesafe.config.ConfigFactory
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.Json

object BalanceRepository:

  val config = ConfigFactory.load()
  val url    = config.getString("ctx.getBalanceapi_url")

  def getBalanceOption[F[_]: Async]: EitherT[F, String, Option[String]] =
    val result = Source.fromURL(url).mkString
    EitherT.rightT(Some(result))

  def getBalance =
    val balanceJson = Source.fromURL(url).mkString
    parse(balanceJson)
      .getOrElse(Json.Null)
      .hcursor
      .downField("result")
      .as[String]
      .getOrElse("")
