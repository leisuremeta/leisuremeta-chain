package io.leisuremeta.chain.gateway.eth.common

import io.circe.generic.auto.*
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.given

object GatewayApi:

  final case class GatewayRequest(
    key: String,
    doublyEncryptedFrontPartBase64: String,
  )

  final case class GatewayResponse(
    singlyEncryptedBase64: String,
  )

  sealed trait GatewayApiError

  final case class ServerError(msg: String) extends GatewayApiError

  sealed trait UserError extends GatewayApiError:
    def msg: String
  final case class Unauthorized(msg: String) extends UserError
  final case class NotFound(msg: String)     extends UserError
  final case class BadRequest(msg: String)   extends UserError

  val postDecryptEndpoint: PublicEndpoint[GatewayRequest, GatewayApiError, GatewayResponse, Any] =
    endpoint.post
      .in(jsonBody[GatewayRequest])
      .out(jsonBody[GatewayResponse])
      .errorOut:
        oneOf[GatewayApiError](
          oneOfVariant(statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])),
          oneOfVariant(statusCode(StatusCode.Unauthorized).and(jsonBody[Unauthorized])),
          oneOfVariant(statusCode(StatusCode.NotFound).and(jsonBody[NotFound])),
          oneOfVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ServerError])),
        )
  