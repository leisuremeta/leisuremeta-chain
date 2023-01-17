package io.leisuremeta

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointIO
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}
import io.circe.generic.auto.*
import io.leisuremeta.chain.lmscan.backend.entity.Tx
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.circe.*

object ExploreApi:
  opaque type Utf8 = String
  object Utf8:
    def apply(s: String): Utf8 = s

  extension (u: Utf8) def asString: String = u

  final case class ServerError(msg: String)

  sealed trait UserError:
    def msg: String
  final case class Unauthorized(msg: String) extends UserError
  final case class NotFound(msg: String)     extends UserError
  final case class BadRequest(msg: String)   extends UserError

  val baseEndpoint = endpoint.errorOut(
    oneOf[Either[ServerError, UserError]](
      oneOfVariantFromMatchType(
        StatusCode.Unauthorized,
        jsonBody[Right[ServerError, Unauthorized]]
          .description("invalid signature"),
      ),
      oneOfVariantFromMatchType(
        StatusCode.NotFound,
        jsonBody[Right[ServerError, NotFound]]
          .description("not found"),
      ),
      oneOfVariantFromMatchType(
        StatusCode.BadRequest,
        jsonBody[Right[ServerError, BadRequest]]
          .description("bad request"),
      ),
      oneOfVariantFromMatchType(
        StatusCode.InternalServerError,
        jsonBody[Left[ServerError, UserError]]
          .description("internal server error"),
      ),
    ),
  )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxEndPoint = baseEndpoint.get
    .in("hello" / "world")
    .in(query[String]("name"))
    .out(jsonBody[String])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxListEndPoint = baseEndpoint.get
    .in("tx" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[Seq[Tx]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxDetail = baseEndpoint.get
    .in("tx")
    .in(path[String]) // tx_hash
    .in("detail")
    .out(jsonBody[Option[Tx]])

// object Test extends App:
//   import io.circe.syntax.*
//   val intsJson = List(1, 2, 3).asJson
//   println(intsJson)
