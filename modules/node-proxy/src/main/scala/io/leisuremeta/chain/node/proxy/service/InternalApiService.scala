package io.leisuremeta.chain
package node.proxy
package service

import io.circe.parser.decode
import sttp.client3.*
import sttp.model.{Uri, StatusCode}
import cats.implicits.*
import cats.effect.*
import cats.effect.Async
import cats.effect.Ref
import cats.effect.kernel.Async
import scala.concurrent.duration.*
import sttp.model.MediaType
import io.leisuremeta.chain.node.proxy.model.TxModel
import io.circe.generic.auto.*

import api.model.*
import api.model.account.EthAddress
import api.model.creator_dao.CreatorDaoId
import api.model.token.*
import api.model.Block.*

object InternalApiService:
  def apply[F[_]: Async](
      backend: SttpBackend[F, Any],
      blocker: Ref[F, Boolean],
      baseUrlsLock: Ref[F, List[String]],
      queue: PostTxQueue[F],
  ): InternalApiService[F] =
    // Async[F].delay(new InternalApiService[F](backend, blocker, baseUrlsLock))
    new InternalApiService[F](backend, blocker, baseUrlsLock, queue)

class InternalApiService[F[_]: Async](
    backend: SttpBackend[F, Any],
    blocker: Ref[F, Boolean],
    baseUrlsLock: Ref[F, List[String]],
    queue: PostTxQueue[F],
):
  // val baseUrl = "http://lmc.leisuremeta.io"
  // val baseUrl = "http://test.chain.leisuremeta.io"

  // def backend[F[_]: Async]: SttpBackend[F, Any] =
  //   ArmeriaCatsBackend.usingClient[F](webClient(SttpBackendOptions.Default))
  // def injectQueue(postQueue: PostTxQueue[F]) =
  //   queue = Some(postQueue)

  def getAsString(
      uri: Uri,
  ): F[(StatusCode, String)] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .get(uri)
        .send(backend)
        .map(res => (res.code, res.body))
    yield
    // scribe.info(s"getAsString result: $result")
    result

  def getAsResponse(
      uri: Uri,
  ): F[(StatusCode, Response[String])] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .get(uri)
        .send(backend)
    yield (result.code, result)

  def getTxAsResponse[A: io.circe.Decoder](
      uri: Uri,
  ): F[(StatusCode, Either[String, A])] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .get(uri)
        .send(backend)
        .map { response =>
          (
            response.code,
            for
              body <- response.body
              a    <- decode[A](body).leftMap(_.getMessage())
            yield a,
          )
        }
    yield
    // scribe.info(s"getAsString result: $result")
    result

  def postAsString(
      uri: Uri,
      body: String,
  ): F[(StatusCode, String)] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .post(uri)
        .body(body)
        .send(backend)
        .map(res => (res.code, res.body))
    yield result

  def postAsResponse(
      uri: Uri,
      body: String,
  ): F[(StatusCode, Response[String])] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .post(uri)
        .contentType(MediaType.ApplicationJson)
        .body(body)
        .send(backend)
    yield (result.code, result)

  def getBlock(
      blockHash: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/block/$blockHash")
        }
      }
      .map(_.head)

  def getAccount(
      account: Account,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/account/${account.utf8.value}")
        }
      }
      .map(_.head)

  def getEthAccount(
      ethAddress: EthAddress,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/eth/$ethAddress")
        }
      }
      .map(_.head)

  def getGroupInfo(
      groupId: GroupId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/group/$groupId")
        }
      }
      .map(_.head)

  def getBlockList(
      fromOption: Option[String],
      limitOption: Option[Int],
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/block?from=${fromOption
              .getOrElse("")}&limit=${limitOption.getOrElse("")}")
        }
      }
      .map(_.head)

  def getStatus: F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/status")
        }
      }
      .map(_.head)

  def getTokenDef(
      tokenDefinitionId: TokenDefinitionId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/token-def/$tokenDefinitionId")
        }
      }
      .map(_.head)

  def getBalance(
      account: Account,
      movable: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/balance/$account?movable=${movable}")
        }
      }
      .map(_.head)

  def getNftBalance(
      account: Account,
      movable: Option[String],
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/nft-balance/$account?movable=${movable}")
        }
      }
      .map(_.head)

  def getToken(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/token/$tokenId")
        }
      }
      .map(_.head)

  def getTokenHistory(
      txHash: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/token-hist/$txHash")
        }
      }
      .map(_.head)

  def getOwners(
      tokenDefinitionId: TokenDefinitionId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/owners/$tokenDefinitionId")
        }
      }
      .map(_.head)

  def getAccountActivity(
      account: Account,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/activity/account/$account")
        }
      }
      .map(_.head)

  def getTokenActivity(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/activity/token/$tokenId")
        }
      }
      .map(_.head)

  def getAccountSnapshot(
      account: Account,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/snapshot/account/$account")
        }
      }
      .map(_.head)

  def getTokenSnapshot(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/snapshot/token/$tokenId")
        }
      }
      .map(_.head)

  def getStatusEndpoint: F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/status")
        }
      }
      .map(_.head)

  def getOwnershipSnapshot(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/snapshot/ownership/$tokenId")
        }
      }
      .map(_.head)

  def getOwnershipSnapshotMap(
      tokenId: Option[TokenId],
      limit: Option[Int],
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(
            uri"$baseUrl/snapshot/ownership?from=$tokenId&limit=$limit",
          )
        }
      }
      .map(_.head)

  def getOwnershipRewarded(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/rewarded/ownership/$tokenId")
        }
      }
      .map(_.head)

  def getReward(
      tokenId: TokenId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/rewarded/ownership/$tokenId")
        }
      }
      .map(_.head)

  def getDaoInfo(groupId: GroupId): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap: urls =>
        urls.traverse: baseUrl =>
          getAsString(uri"$baseUrl/dao/$groupId")
      .map(_.head)

  def getTx(
      txHash: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/tx/$txHash")
        }
      }
      .map(_.head)

  def getTxFromOld(
      txHash: String,
  ): F[(StatusCode, Either[String, TxModel])] =
    baseUrlsLock.get.map(_.head).flatMap { baseUrl =>
      getTxAsResponse[TxModel](uri"$baseUrl/tx/$txHash")
    }

  def getTxSet(
      txHash: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          getAsString(uri"$baseUrl/tx/$txHash")
        }
      }
      .map(_.head)

  def postTx(
      txs: String,
  ): F[(StatusCode, String)] =
    queue.push(txs)
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          postAsString(
            uri"$baseUrl/tx",
            txs,
          )
        }
      }
      .map(_.head)

  def postTx(
      baseUrl: String,
      txs: String,
  ): F[(StatusCode, Response[String])] =
    postAsResponse(
      uri"$baseUrl/tx",
      txs,
    )

  def postTxHash(
      txs: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get
      .flatMap { urls =>
        urls.traverse { baseUrl =>
          postAsString(
            uri"$baseUrl/txhash",
            txs,
          )
        }
      }
      .map(_.head)

  def jobOrBlock: F[Unit] =
    blocker.get.flatMap { value =>
      value match
        case false =>
          Async[F].sleep(3.second)
            >> jobOrBlock
        case true => Async[F].unit
    }

  def bestBlock(baseUri: String): F[(StatusCode, Block)] =
    for
      res <- get[NodeStatus](uri"$baseUri/status")
      (_, nodeStatus) = res
      bestBlock <- block(baseUri, nodeStatus.bestHash.toUInt256Bytes.toHex)
    yield bestBlock

  def block(baseUri: String, blockHash: String): F[(StatusCode, Block)] =
    get[Block](uri"$baseUri/block/$blockHash")

  def postTxs(baseUri: String, body: String): F[(StatusCode, String)] =
    postAsString(uri"$baseUri/tx", body)

  def getAsOption[A: io.circe.Decoder](uri: Uri): F[Option[A]] =
    basicRequest
      .get(uri)
      .send(backend)
      .map { response =>
        response.body.toOption.flatMap { body =>
          decode[A](body).toOption
        }
      }

  def get[A: io.circe.Decoder](uri: Uri): F[(StatusCode, A)] =
    basicRequest
      .get(uri)
      .send(backend)
      .flatMap { response =>
        response.body match
          case Right(body) =>
            decode[A](body) match
              case Right(a)    => Async[F].pure((response.code, a))
              case Left(error) => Async[F].raiseError(error)
          case Left(error) =>
            Async[F].raiseError(
              new Exception(s"Error in response body: $error"),
            )
      }

  def getSnapshotState(
      tokenDefinitionId: TokenDefinitionId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/snapshot-state/$tokenDefinitionId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getFungibleSnapshotBalance(
      account: Account,
      tokenDefinitionId: TokenDefinitionId,
      snapshotId: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri =
            uri"$baseUrl/snapshot-balance/$account/$tokenDefinitionId/$snapshotId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getNftSnapshotBalance(
      account: Account,
      tokenDefinitionId: TokenDefinitionId,
      snapshotId: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri =
            uri"$baseUrl/nft-snapshot-balance/$account/$tokenDefinitionId/$snapshotId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getVoteProposal(
      proposalId: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/vote/proposal/$proposalId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getAccountVotes(
      proposalId: String,
      account: Account,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/vote/account/$proposalId/$account"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getVoteCount(
      proposalId: String,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/vote/count/$proposalId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getCreatorDaoInfo(
      creatorDaoId: CreatorDaoId,
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/creator-dao/$creatorDaoId"
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }

  def getCreatorDaoMember(
      creatorDaoId: CreatorDaoId,
      from: Option[Account],
      limit: Option[Int],
  ): F[(StatusCode, String)] =
    baseUrlsLock.get.flatMap { urls =>
      urls.headOption match
        case Some(baseUrl) =>
          val uri = uri"$baseUrl/creator-dao/$creatorDaoId/member"
            .addParam("from", from.map(_.utf8.value).getOrElse(""))
            .addParam("limit", limit.map(_.toString).getOrElse(""))
          getAsString(uri)
        case None =>
          Async[F].pure(
            (StatusCode.ServiceUnavailable, "No base URL available"),
          )
    }
