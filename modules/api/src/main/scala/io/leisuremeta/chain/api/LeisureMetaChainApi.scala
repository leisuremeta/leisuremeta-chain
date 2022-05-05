package io.leisuremeta.chain
package api

import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.Dispatcher

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import sttp.tapir.server.ServerEndpoint

import lib.datatype.{BigNat, UInt256Bytes}
import api.model.Transaction

object LeisureMetaChainApi:

  given Schema[UInt256Bytes] = Schema.string
  given Schema[BigNat] = Schema.schemaForBigInt.map[BigNat] {
    (bigint: BigInt) => BigNat.fromBigInt(bigint).toOption
  } { (bignat: BigNat) => bignat.toBigInt }
  given [K: KeyEncoder, V: Schema]: Schema[Map[K, V]] =
    Schema.schemaForMap[K, V](KeyEncoder[K].apply)

//  import UInt256Bytes.given
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    endpoint.post
      .in("tx")
      .in(jsonBody[Transaction])
      .out(jsonBody[UInt256Bytes])

  // 0-cost wrapper for a security token
  opaque type Token = String
  object Token:
    def apply(t: String): Token = t

  // model of security & regular inputs/outputs
  case class AuthenticationFailure(reason: String)
  case class AuthenticationSuccess(userId: Int)
  case class LogicFailure(reason: String)
  case class LogicSuccess(message: String)

  // base endpoint with the structure of security defined; a blueprint for other endpoints
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Any",
      "org.wartremover.warts.RedundantConversions",
    ),
  )
  val secureEndpoint: Endpoint[Token, Unit, AuthenticationFailure, Unit, Any] =
    endpoint
      .securityIn(auth.bearer[String]().map(Token(_))(_.toString))
      .errorOut(statusCode(StatusCode.Forbidden))
      .errorOut(jsonBody[AuthenticationFailure])

  // full endpoint, corresponds to: GET /hello/world?name=...; Authentication: Bearer ...
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val helloEndpoint: Endpoint[
    Token,
    String,
    AuthenticationFailure | LogicFailure,
    LogicSuccess,
    Any,
  ] =
    secureEndpoint.get
      .in("hello" / "world")
      .in(query[String]("name"))
      .errorOutVariant(oneOfVariant(jsonBody[LogicFailure]))
      .out(jsonBody[LogicSuccess])

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  val helloServerEndpoint: ServerEndpoint[Any, IO] =
    helloEndpoint
      .serverSecurityLogicPure(token =>
        if token.startsWith("secret")
        then Right(AuthenticationSuccess(token.length))
        else Left(AuthenticationFailure("wrong token")),
      )
      .serverLogicPure { authResult => name =>
        if name === "Gargamel"
        then Left(LogicFailure("wrong name"))
        else Right(LogicSuccess(s"Hello, $name (${authResult.userId})!"))
      }
