package io.leisuremeta.chain
package api

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import io.circe.refined.*
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.CodecFormat.Json
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import lib.crypto.{Hash, Signature}
import lib.datatype.{BigNat, UInt256BigInt, UInt256Bytes, Utf8}
import api.model.{
  Account,
  AccountSignature,
  NodeStatus,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.api_model.AccountInfo
import api.model.Signed.TxHash.given

object LeisureMetaChainApi:

  given Schema[UInt256Bytes]  = Schema.string
  given Schema[UInt256BigInt] = Schema(SchemaType.SInteger())
  given Schema[BigNat] = Schema.schemaForBigInt.map[BigNat] {
    (bigint: BigInt) => BigNat.fromBigInt(bigint).toOption
  } { (bignat: BigNat) => bignat.toBigInt }
  given Schema[Utf8] = Schema.string
  given [K: KeyEncoder, V: Schema]: Schema[Map[K, V]] =
    Schema.schemaForMap[K, V](KeyEncoder[K].apply)
  given [A]: Schema[Hash.Value[A]] = Schema.string
  given Schema[Signature.Header]   = Schema(SchemaType.SInteger())

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
        jsonBody[Right[ServerError, NotFound]].description("not found"),
      ),
      oneOfVariantFromMatchType(
        StatusCode.BadRequest,
        jsonBody[Right[ServerError, BadRequest]].description("bad request"),
      ),
      oneOfVariantFromMatchType(
        StatusCode.InternalServerError,
        jsonBody[Left[ServerError, UserError]].description(
          "internal server error",
        ),
      ),
    ),
  )

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxEndpoint = baseEndpoint.get
    .in("tx" / path[Signed.TxHash])
    .out(jsonBody[TransactionWithResult])


  //  import UInt256Bytes.given
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    baseEndpoint.post
      .in("tx")
      .in(jsonBody[Signed.Tx])
      .out(jsonBody[Signed.TxHash])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint =
    baseEndpoint.get.in("status").out(jsonBody[NodeStatus])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountEndpoint =
    baseEndpoint.get
      .in("account" / path[Account])
      .out(jsonBody[AccountInfo])
