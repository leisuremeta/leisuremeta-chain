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

  val config          = ConfigFactory.load()
  val contractaddress = config.getString("ctx.contractaddress")
  val address         = config.getString("ctx.address").split(",")
  val apikey          = config.getString("ctx.apikey")

  def urlFunction = (
    address: String
  ) =>
    s"https://api.etherscan.io/api?module=account&action=tokenbalance&contractaddress=${contractaddress}&address=${address}&apikey=${apikey}"

  def getBalanceFromUrl = (address: String) =>

    val resultJsonString =
      Source.fromURL(urlFunction(address)).mkString

    parse(resultJsonString)
      .getOrElse(Json.Null)
      .hcursor
      .downField("result")
      .as[String]
      .getOrElse("0")

  def getBalance =
    address
      .foldRight(BigDecimal(0))((url, acc) =>
        BigDecimal(getBalanceFromUrl(url)) + acc,
      )
      .toString()
