package io.leisuremeta

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointIO
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}
import io.circe.generic.auto.*
import io.leisuremeta.chain.lmscan.backend.model.PageResponse
import io.leisuremeta.chain.lmscan.backend.model.PageNavigation
import io.leisuremeta.chain.lmscan.backend.model.AccountDetail
import io.leisuremeta.chain.lmscan.backend.model.NftDetail
import io.leisuremeta.chain.lmscan.backend.model.{TxDetail, TxInfo, BlockInfo, BlockDetail}
import io.leisuremeta.chain.lmscan.backend.entity.Summary

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
  val getTxPageEndPoint = baseEndpoint.get
    .in("tx" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .in(
      query[Option[String]]("accountAddr")
        .and(query[Option[String]]("blockHash")),
    )
    .out(jsonBody[PageResponse[TxInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxDetailEndPoint = baseEndpoint.get
    .in("tx")
    .in(path[String]) // tx_hash
    .in("detail")
    .out(jsonBody[Option[TxDetail]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockPageEndPoint = baseEndpoint.get
    .in("block" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[PageResponse[BlockInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockDetailEndPoint = baseEndpoint.get
    .in("block")
    .in(path[String]) // block_hash
    .in("detail")
    .out(jsonBody[Option[BlockDetail]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountDetailEndPoint = baseEndpoint.get
    .in("account")
    .in(path[String]("accountAddr")) // account_address
    .in("detail")
    .out(jsonBody[Option[AccountDetail]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getNftDetailEndPoint = baseEndpoint.get
    .in("nft")
    .in(path[String]("tokenId")) // token_id
    .in("detail")
    .out(jsonBody[Option[NftDetail]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getSummaryMainEndPoint = baseEndpoint.get
    .in("summary")
    .in("main")
    .out(jsonBody[Option[Summary]])

  // @SuppressWarnings(Array("org.wartremover.warts.Any"))
  // val getSearchTargetType = baseEndpoint.get
  //   .in("search")
  //   .in(query[String]("target")) // targetValue
  //   .out(jsonBody[Option[String]])
