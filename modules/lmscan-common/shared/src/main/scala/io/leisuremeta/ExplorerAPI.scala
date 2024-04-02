package io.leisuremeta.chain.lmscan.common

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.EndpointIO
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}
import io.circe.generic.auto.*
import io.leisuremeta.chain.lmscan.common.model._
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

  val getTxPageEndPoint = baseEndpoint.get
    .in("tx" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .in(
      query[Option[String]]("accountAddr")
        .and(query[Option[String]]("blockHash"))
        .and(query[Option[String]]("subtype")),
    )
    .out(jsonBody[PageResponse[TxInfo]])

  val getTxDetailEndPoint = baseEndpoint.get
    .in("tx")
    .in(path[String]) // tx_hash
    .in("detail")
    .out(jsonBody[Option[TxDetail]])

  val getBlockPageEndPoint = baseEndpoint.get
    .in("block" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[PageResponse[BlockInfo]])

  val getBlockDetailEndPoint = baseEndpoint.get
    .in("block")
    .in(path[String]) // block_hash
    .in("detail")
    .in(query[Option[Int]]("p"))
    .out(jsonBody[Option[BlockDetail]])

  val getAccountPageEndPoint = baseEndpoint.get
    .in("account" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[PageResponse[AccountInfo]])

  val getAccountDetailEndPoint = baseEndpoint.get
    .in("account")
    .in(path[String]("accountAddr"))
    .in("detail")
    .in(query[Option[Int]]("p"))
    .out(jsonBody[Option[AccountDetail]])

  val getNftPageEndPoint = baseEndpoint.get
    .in("nft" / "list")
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[PageResponse[NftInfoModel]])
  
  val getNftSeasonEndPoint = baseEndpoint.get
    .in("nft")
    .in(path[String]("season"))
    .in(
      sttp.tapir.EndpointInput.derived[PageNavigation],
    )
    .out(jsonBody[PageResponse[NftSeasonModel]])

  val getNftDetailEndPoint = baseEndpoint.get
    .in("nft")
    .in(path[String]("tokenId"))
    .in("detail")
    .out(jsonBody[Option[NftDetail]])

  val getNftOwnerInfoEndPoint = baseEndpoint.get
    .in("nft")
    .in(path[String]("tokenId"))
    .in("info")
    .out(jsonBody[Option[NftOwnerInfo]])

  val getSummaryMainEndPoint = baseEndpoint.get
    .in("summary")
    .in("main")
    .out(jsonBody[Option[SummaryBoard]])

  val getSummaryChartEndPoint = baseEndpoint.get
    .in("summary")
    .in("chart")
    .in(path[String]("chartType"))
    .out(jsonBody[SummaryChart])

  val getTotalBalance = baseEndpoint.get
    .in("total")
    .in("balance")
    .out(jsonBody[Option[String]])

  val getValanceFromChainProd = baseEndpoint.get
    .in("prod")
    .in("chain")
    .out(jsonBody[Option[String]])
