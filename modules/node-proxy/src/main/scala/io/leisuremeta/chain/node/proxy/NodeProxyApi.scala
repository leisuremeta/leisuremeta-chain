package io.leisuremeta.chain.node
package proxy

import java.time.Instant
import java.util.Locale

import io.circe.KeyEncoder
import io.circe.generic.auto.*
import io.circe.refined.*
import scodec.bits.ByteVector
import sttp.client3.*
import sttp.model.StatusCode
import sttp.tapir.CodecFormat.{Json, TextPlain}
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.{*, given}
import sttp.tapir.EndpointIO.annotations.body
import sttp.tapir.*
import io.leisuremeta.chain.api.model.*
import io.leisuremeta.chain.api.model.api_model.*
import io.leisuremeta.chain.api.model.account.EthAddress
import io.leisuremeta.chain.api.model.token.*
import sttp.tapir.Codec.PlainCodec
import sttp.model.MediaType
import sttp.tapir.Codec.JsonCodec
import io.circe.Json
import sttp.tapir.EndpointIO.annotations.jsonbody


// import lib.crypto.{Hash, Signature}
// import lib.datatype.{BigNat, UInt256, UInt256BigInt, UInt256Bytes, Utf8}
// import api.model.{
//   Account,
//   AccountSignature,
//   Block,
//   GroupId,
//   NodeStatus,
//   Signed,
//   Transaction,
//   TransactionWithResult,
// }
// import api.model.account.EthAddress
// import api.model.api_model.{
//   AccountInfo,
//   ActivityInfo,
//   BalanceInfo,
//   BlockInfo,
//   GroupInfo,
//   NftBalanceInfo,
//   RewardInfo,
//   TxInfo,
// }
// import api.model.token.{NftState, TokenDefinition, TokenDefinitionId, TokenId}
// import api.model.reward.{ActivitySnapshot, OwnershipSnapshot, OwnershipRewardLog}
// import api.model.Signed.TxHash.given

object NodeProxyApi:
  val jsonType = MediaType.ApplicationJson.toString

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxSetEndpoint = endpoint.get
    .in("tx" / query[String]("block"))
    .out(stringJsonBody)
    .out(header("Content-Type", jsonType))
  
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxEndpoint = endpoint.get
    .in("tx" / path[String])
    .out(stringJsonBody)
    .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    endpoint.post
      .in("tx")
      .in(stringBody)
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxHashEndpoint = endpoint.post
    .in("txhash")
    .in(stringBody)
    .out(stringJsonBody)

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint =
    endpoint.get.in("status")
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountEndpoint =
    endpoint.get
      .in("account" / path[Account])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getEthEndpoint =
    endpoint.get
      .in("eth" / path[EthAddress])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getGroupEndpoint =
    endpoint.get
      .in("group" / path[GroupId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockListEndpoint =
    endpoint.get
      .in(
        "block" / query[Option[String]]("from")
          .and(query[Option[Int]]("limit")),
      )
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockEndpoint =
    endpoint.get
      .in("block" / path[String])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenDefinitionEndpoint =
    endpoint.get
      .in("token-def" / path[TokenDefinitionId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBalanceEndpoint =
    endpoint.get
      .in("balance" / path[Account].and(query[String]("movable")))
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getNftBalanceEndpoint =
    endpoint.get
      .in("nft-balance" / path[Account].and(query[Option[String]]("movable")))
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenEndpoint =
    endpoint.get
      .in("token" / path[TokenId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnersEndpoint =
    endpoint.get
      .in("owners" / path[TokenDefinitionId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountActivityEndpoint =
    endpoint.get
      .in("activity" / "account" / path[Account])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenActivityEndpoint =
    endpoint.get
      .in("activity" / "token" / path[TokenId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "account" / path[Account])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "token" / path[TokenId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "ownership" / path[TokenId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotMapEndpoint =
    endpoint.get
      .in {
        "snapshot" / "ownership" / query[Option[TokenId]]("from")
          .and(query[Option[Int]]("limit"))
      }
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipRewardedEndpoint =
    endpoint.get
      .in("rewarded" / "ownership" / path[TokenId])
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getRewardEndpoint =
    endpoint.get
      .in {
        "reward" / path[Account]
          .and(query[Option[Instant]]("timestamp"))
          .and(query[Option[Account]]("dao-account"))
          .and(query[Option[String]]("reward-amount"))
      }
      .out(stringJsonBody)
      .out(header("Content-Type", jsonType))

  // enum Movable:
  //   case Free, Locked
  // object Movable:
  //   @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  //   given Codec[String, Movable, TextPlain] = Codec.string.mapDecode {
  //     (s: String) =>
  //       s match
  //         case "free"   => DecodeResult.Value(Movable.Free)
  //         case "locked" => DecodeResult.Value(Movable.Locked)
  //         case _ => DecodeResult.Error(s, new Exception(s"invalid movable: $s"))
  //   }(_.toString.toLowerCase(Locale.ENGLISH))

  //   @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  //   given Codec[String, Option[Movable], TextPlain] = Codec.string.mapDecode {
  //     (s: String) =>
  //       s match
  //         case "free"   => DecodeResult.Value(Some(Movable.Free))
  //         case "locked" => DecodeResult.Value(Some(Movable.Locked))
  //         case "all"    => DecodeResult.Value(None)
  //         case _ => DecodeResult.Error(s, new Exception(s"invalid movable: $s"))
  //   }(_.fold("")(_.toString.toLowerCase(Locale.ENGLISH)))
