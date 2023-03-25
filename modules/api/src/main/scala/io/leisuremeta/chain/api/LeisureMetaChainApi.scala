package io.leisuremeta.chain
package api

import java.time.Instant
import java.util.Locale

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import io.circe.refined.*
import scodec.bits.ByteVector
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.CodecFormat.{Json, TextPlain}
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}

import lib.crypto.{Hash, Signature}
import lib.datatype.{BigNat, UInt256, UInt256BigInt, UInt256Bytes, Utf8}
import api.model.{
  Account,
  AccountSignature,
  Block,
  GroupId,
  NodeStatus,
  Signed,
  Transaction,
  TransactionWithResult,
}
import api.model.account.EthAddress
import api.model.api_model.{
  AccountInfo,
  ActivityInfo,
  BalanceInfo,
  BlockInfo,
  GroupInfo,
  NftBalanceInfo,
  RewardInfo,
  TxInfo,
}
import api.model.token.{NftState, TokenDefinition, TokenDefinitionId, TokenId}
import api.model.reward.{ActivitySnapshot, OwnershipSnapshot, OwnershipRewardLog}
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
  given Schema[Transaction]        = Schema.derived[Transaction]

  given hashValueCodec[A]: Codec[String, Hash.Value[A], TextPlain] =
    Codec.string.mapDecode { (s: String) =>
      ByteVector
        .fromHexDescriptive(s)
        .left
        .map(new Exception(_))
        .flatMap(UInt256.from) match
        case Left(e)  => DecodeResult.Error(s, e)
        case Right(v) => DecodeResult.Value(Hash.Value(v))
    }(_.toUInt256Bytes.toBytes.toHex)
  given bignatCodec: Codec[String, BigNat, TextPlain] =
    Codec.bigInt.mapDecode { (n: BigInt) =>
      BigNat.fromBigInt(n) match
        case Left(e)  => DecodeResult.Error(n.toString(10), new Exception(e))
        case Right(v) => DecodeResult.Value(v)
    }(_.toBigInt)

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
  val getTxSetEndpoint = baseEndpoint.get
    .in("tx" / query[Block.BlockHash]("block"))
    .out(jsonBody[Set[TxInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxEndpoint = baseEndpoint.get
    .in("tx" / path[Signed.TxHash])
    .out(jsonBody[TransactionWithResult])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    baseEndpoint.post
      .in("tx")
      .in(jsonBody[Seq[Signed.Tx]])
      .out(jsonBody[Seq[Hash.Value[TransactionWithResult]]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxHashEndpoint = baseEndpoint.post
    .in("txhash")
    .in(jsonBody[Seq[Transaction]])
    .out(jsonBody[Seq[Hash.Value[Transaction]]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint =
    baseEndpoint.get.in("status").out(jsonBody[NodeStatus])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountEndpoint =
    baseEndpoint.get
      .in("account" / path[Account])
      .out(jsonBody[AccountInfo])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getEthEndpoint =
    baseEndpoint.get
      .in("eth" / path[EthAddress])
      .out(jsonBody[Account])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getGroupEndpoint =
    baseEndpoint.get
      .in("group" / path[GroupId])
      .out(jsonBody[GroupInfo])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockListEndpoint =
    baseEndpoint.get
      .in(
        "block" / query[Option[Block.BlockHash]]("from")
          .and(query[Option[Int]]("limit")),
      )
      .out(jsonBody[List[BlockInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockEndpoint =
    baseEndpoint.get
      .in("block" / path[Block.BlockHash])
      .out(jsonBody[Block])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenDefinitionEndpoint =
    baseEndpoint.get
      .in("token-def" / path[TokenDefinitionId])
      .out(jsonBody[TokenDefinition])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBalanceEndpoint =
    baseEndpoint.get
      .in("balance" / path[Account].and(query[Movable]("movable")))
      .out(jsonBody[Map[TokenDefinitionId, BalanceInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getNftBalanceEndpoint =
    baseEndpoint.get
      .in("nft-balance" / path[Account].and(query[Option[Movable]]("movable")))
      .out(jsonBody[Map[TokenId, NftBalanceInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenEndpoint =
    baseEndpoint.get
      .in("token" / path[TokenId])
      .out(jsonBody[NftState])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnersEndpoint =
    baseEndpoint.get
      .in("owners" / path[TokenDefinitionId])
      .out(jsonBody[Map[TokenId, Account]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountActivityEndpoint =
    baseEndpoint.get
      .in("activity" / "account" / path[Account])
      .out(jsonBody[Seq[ActivityInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenActivityEndpoint =
    baseEndpoint.get
      .in("activity" / "token" / path[TokenId])
      .out(jsonBody[Seq[ActivityInfo]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountSnapshotEndpoint =
    baseEndpoint.get
      .in("snapshot" / "account" / path[Account])
      .out(jsonBody[ActivitySnapshot])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenSnapshotEndpoint =
    baseEndpoint.get
      .in("snapshot" / "token" / path[TokenId])
      .out(jsonBody[ActivitySnapshot])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotEndpoint =
    baseEndpoint.get
      .in("snapshot" / "ownership" / path[TokenId])
      .out(jsonBody[OwnershipSnapshot])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotMapEndpoint =
    baseEndpoint.get
      .in {
        "snapshot" / "ownership" / query[Option[TokenId]]("from")
          .and(query[Option[Int]]("limit"))
      }
      .out(jsonBody[Map[TokenId, OwnershipSnapshot]])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipRewardedEndpoint =
    baseEndpoint.get
      .in("rewarded" / "ownership" / path[TokenId])
      .out(jsonBody[OwnershipRewardLog])

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getRewardEndpoint =
    baseEndpoint.get
      .in {
        "reward" / path[Account]
          .and(query[Option[Instant]]("timestamp"))
          .and(query[Option[Account]]("dao-account"))
          .and(query[Option[BigNat]]("reward-amount"))
      }
      .out(jsonBody[RewardInfo])

  enum Movable:
    case Free, Locked
  object Movable:
    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    given Codec[String, Movable, TextPlain] = Codec.string.mapDecode {
      (s: String) =>
        s match
          case "free"   => DecodeResult.Value(Movable.Free)
          case "locked" => DecodeResult.Value(Movable.Locked)
          case _ => DecodeResult.Error(s, new Exception(s"invalid movable: $s"))
    }(_.toString.toLowerCase(Locale.ENGLISH))

    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    given Codec[String, Option[Movable], TextPlain] = Codec.string.mapDecode {
      (s: String) =>
        s match
          case "free"   => DecodeResult.Value(Some(Movable.Free))
          case "locked" => DecodeResult.Value(Some(Movable.Locked))
          case "all"    => DecodeResult.Value(None)
          case _ => DecodeResult.Error(s, new Exception(s"invalid movable: $s"))
    }(_.fold("")(_.toString.toLowerCase(Locale.ENGLISH)))
