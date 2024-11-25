package io.leisuremeta.chain
package node
package proxy

import java.time.Instant

import sttp.model.MediaType
import sttp.tapir.*

import api.model.*
import api.model.account.EthAddress
import api.model.token.*
import api.model.creator_dao.*

object NodeProxyApi:
  val jsonType = MediaType.ApplicationJson.toString

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxSetEndpoint = endpoint.get
    .in("tx" / query[String]("block"))
    .out(statusCode.and(stringJsonBody))
    .out(header("Content-Type", jsonType))

  
  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTxEndpoint = endpoint.get
    .in("tx" / path[String])
    .out(statusCode.and(stringJsonBody))
    .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxEndpoint =
    endpoint.post
      .in("tx")
      .in(stringBody)
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val postTxHashEndpoint = endpoint.post
    .in("txhash")
    .in(stringBody)
    .out(statusCode.and(stringJsonBody))
    .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getStatusEndpoint =
    endpoint.get.in("status")
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountEndpoint =
    endpoint.get
      .in("account" / path[Account])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getEthEndpoint =
    endpoint.get
      .in("eth" / path[EthAddress])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getGroupEndpoint =
    endpoint.get
      .in("group" / path[GroupId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockListEndpoint =
    endpoint.get
      .in:
        "block" / query[Option[String]]("from")
          .and(query[Option[Int]]("limit"))
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBlockEndpoint =
    endpoint.get
      .in("block" / path[String])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenDefinitionEndpoint =
    endpoint.get
      .in("token-def" / path[TokenDefinitionId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getBalanceEndpoint =
    endpoint.get
      .in("balance" / path[Account].and(query[String]("movable")))
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getNftBalanceEndpoint =
    endpoint.get
      .in("nft-balance" / path[Account].and(query[Option[String]]("movable")))
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenEndpoint =
    endpoint.get
      .in("token" / path[TokenId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenHistoryEndpoint =
    endpoint.get
      .in("token-hist" / path[String])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnersEndpoint =
    endpoint.get
      .in("owners" / path[TokenDefinitionId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountActivityEndpoint =
    endpoint.get
      .in("activity" / "account" / path[Account])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenActivityEndpoint =
    endpoint.get
      .in("activity" / "token" / path[TokenId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "account" / path[Account])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getTokenSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "token" / path[TokenId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotEndpoint =
    endpoint.get
      .in("snapshot" / "ownership" / path[TokenId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipSnapshotMapEndpoint =
    endpoint.get
      .in:
        "snapshot" / "ownership" / query[Option[TokenId]]("from")
          .and(query[Option[Int]]("limit"))
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getOwnershipRewardedEndpoint =
    endpoint.get
      .in("rewarded" / "ownership" / path[TokenId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getRewardEndpoint =
    endpoint.get
      .in:
        "reward" / path[Account]
          .and(query[Option[Instant]]("timestamp"))
          .and(query[Option[Account]]("dao-account"))
          .and(query[Option[String]]("reward-amount"))
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getDaoInfoEndpoint =
    endpoint.get
      .in("dao" / path[GroupId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getSnapshotStateEndpoint =
    endpoint.get
      .in("snapshot-state" / path[TokenDefinitionId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getFungibleSnapshotBalanceEndpoint =
    endpoint.get
      .in:
        "snapshot-balance" / path[Account] / path[TokenDefinitionId] / path[String]
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getNftSnapshotBalanceEndpoint =
    endpoint.get
      .in:
        "nft-snapshot-balance" / path[Account] / path[TokenDefinitionId] / path[String]
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getVoteProposalEndpoint =
    endpoint.get
      .in("vote" / "proposal" / path[String])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getAccountVotesEndpoint =
    endpoint.get
      .in("vote" / "account" / path[String] / path[Account])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getVoteCountEndpoint =
    endpoint.get
      .in("vote" / "count" / path[String])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getCreatorDaoInfoEndpoint =
    endpoint.get
      .in("creator-dao" / path[CreatorDaoId])
      .out(statusCode.and(stringJsonBody))
      .out(header("Content-Type", jsonType))

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  val getCreatorDaoMemberEndpoint =
    endpoint.get
      .in:
        "creator-dao" / path[CreatorDaoId] / "member"
          .and(query[Option[Account]]("from"))
          .and(query[Option[Int]]("limit"))
      .out(statusCode.and(stringJsonBody))
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
