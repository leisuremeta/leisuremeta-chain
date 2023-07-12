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

object PlaynommBalanceRepository:

  val config   = ConfigFactory.load()
  val url_dev  = config.getString("ctx.devChain_url")
  val url_prod = config.getString("ctx.lmChain_url")
  val toggle   = config.getString("ctx.url")

  val toggle_url = toggle.contains("prod") match
    // 상용서버 => 상용체인
    case true => url_prod
    // 개발서버 => 개발체인
    case _ => url_dev

  def getBalance[F[_]: Async]: EitherT[F, String, Option[String]] =
    val balanceJson = Source.fromURL(toggle_url).mkString
    val parsed = parse(balanceJson)
      .getOrElse(Json.Null)
      .hcursor
      .downField("LM")
      .downField("totalAmount")
      .as[BigDecimal]
      .getOrElse(0)

    EitherT.rightT(Some(s"$parsed"))

  def getBalance =
    val balanceJson = Source.fromURL(toggle_url).mkString
    val parsed = parse(balanceJson)
      .getOrElse(Json.Null)
      .hcursor
      .downField("LM")
      .downField("totalAmount")
      .as[BigDecimal]
      .getOrElse(BigDecimal(0))
    parsed
