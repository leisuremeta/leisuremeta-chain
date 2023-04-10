package io.leisuremeta.chain.node.proxy
package service

import io.leisuremeta.chain.api.model.api_model.*
import io.leisuremeta.chain.api.model.*
import io.circe.parser.decode
import io.circe.generic.auto.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.*
import sttp.model.{Uri, StatusCode}
import sttp.tapir.EndpointIO.annotations.body
import com.linecorp.armeria.client.WebClient
import com.linecorp.armeria.client.encoding.DecodingClient
import com.linecorp.armeria.client.ClientFactory
import cats.effect.IO
import cats.implicits.*
import cats.syntax.functor._
import cats.syntax.*
import cats.{~>, Monad}
import cats.data.EitherT
import cats.effect.unsafe.IORuntime
import io.leisuremeta.chain.api.model.account.EthAddress
import io.leisuremeta.chain.api.model.api_model.BlockInfo
import io.leisuremeta.chain.node.proxy.{NodeProxyApi as Api}
import cats.data.Op
import io.leisuremeta.chain.api.model.reward.*
import io.leisuremeta.chain.api.model.token.*
// import io.leisuremeta.chain.api.model.Transaction.RewardTx.OfferReward
import cats.effect.*
import cats.effect.Async
import cats.effect.std.Semaphore
import cats.effect.std.AtomicCell
import cats.syntax.all._
import cats.effect.kernel.Async
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import cats.effect.Ref


class InternalApiService[F[_]: Async](
  backend: SttpBackend[F, Any],
  blocker: Ref[F, Boolean],
):
  // val BASE_URL = "http://lmc.leisuremeta.io"
  val BASE_URL = "http://test.chain.leisuremeta.io"
  
  // def backend[F[_]: Async]: SttpBackend[F, Any] = 
  //   ArmeriaCatsBackend.usingClient[F](webClient(SttpBackendOptions.Default))
  
  // given runtime: IORuntime = cats.effect.unsafe.implicits.global

  def getAsString(
    uri: Uri /*, backend: SttpBackend[F, Any] */,
    // flag: F[Ref[F, Boolean]]
  ): F[String] = 
    // blocker.flatMap { ref =>
      for {
        _ <- jobOrBlock
        result <- basicRequest
          .response(asStringAlways)
          .get(uri)
          .send(backend)
          .map(_.body)
      } yield result
    // }

  def postAsString(
    uri: Uri,
    body: String,
  ): F[String] =
    for
      _ <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .post(uri)
        .body(body)
        .send(backend)
        .map(_.body)
    yield result


  /*
  def get[F[_]: Async, A: io.circe.Decoder](
    uri: Uri /*, backend: SttpBackend[F, Any] */
  ): F[Option[A]] = 
    basicRequest
      .response(asStringAlways)
      .get(uri)
      .send(backend)
      .map { response =>
        if response.code.isSuccess then
          decode[A](response.body) match
            case Right(value) => Some(value)
            case Left(error) =>
              scribe.error(s"data is not found: ${response.body}")
              None
        else if response.code.code == StatusCode.NotFound.code then
          scribe.info(s"data is not found: ${response.body}")
          None
        else 
          scribe.error(s"Error getting data: ${response.body}")
          None
      }

  def getAsEither[F[_]: Async, A: io.circe.Decoder](
    uri: Uri /*, backend: SttpBackend[F, Any] */
  ): F[Either[String, A]] = 
    basicRequest
      .response(asStringAlways)
      .get(uri)
      .send(backend)
      .map { response =>
        if response.code.isSuccess then
          decode[A](response.body) match
            case Right(value) => Right(value)
            case Left(error) =>
              scribe.error(s"data is not found: ${response.body}")
              Left(response.body)
        else if response.code.code == StatusCode.NotFound.code then
          scribe.info(s"data is not found: ${response.body}")
          Left(response.body)
        else 
          scribe.error(s"Error getting data: ${response.body}")
          Left(response.statusText)
      }
  */
  
  

  def getBlock(
    blockHash: String,
  ): F[String] =
    getAsString(uri"$BASE_URL/block/$blockHash")


  def getAccount(
    account: Account
  ): F[String] = 
    getAsString(uri"$BASE_URL/account/${account.utf8.value}") 
    
  def getEthAccount(
    ethAddress: EthAddress
  ): F[String] =
    getAsString(uri"$BASE_URL/eth/$ethAddress")
  
  def getGroupInfo(
    groupId: GroupId
  ): F[String] =
    getAsString(uri"$BASE_URL/group/$groupId")


  def getBlockList(
    fromOption: Option[String],
    limitOption: Option[String],
  ): F[String] =
    getAsString(uri"$BASE_URL/block?from=${fromOption.getOrElse("")}&limit=${limitOption.getOrElse("")}")

  def getStatus:
     F[String] =
    getAsString(uri"$BASE_URL/status")

  def getTokenDef(
    tokenDefinitionId: TokenDefinitionId
  ): F[String] =
    getAsString(uri"$BASE_URL/token-def/$tokenDefinitionId")

  def getBalance(
    account: Account,
    movable: String
  ): F[String] =
    getAsString(uri"$BASE_URL/balance/$account?movable=${movable}")

  def getNftBalance(
    account: Account,
    movable: Option[String]
  ): F[String] =
    getAsString(uri"$BASE_URL/nft-balance/$account?movable=${movable}")


  def getToken(
    tokenId: TokenId
  ): F[String] =
    getAsString(uri"$BASE_URL/token/$tokenId")

  def getOwners(
    tokenDefinitionId: TokenDefinitionId
  ): F[String] =
    getAsString(uri"$BASE_URL/owners/$tokenDefinitionId")

  def getAccountActivity(
    account: Account
  ): F[String] =
    getAsString(
      uri"$BASE_URL/activity/account/$account"
    )

  def getTokenActivity(
    tokenId: TokenId
  ): F[String] =
    getAsString(
      uri"$BASE_URL/activity/token/$tokenId"
    )

  def getAccountSnapshot(
    account: Account
  ): F[String] = 
    getAsString(
      uri"$BASE_URL/snapshot/account/$account"
    )

  def getTokenSnapshot(
    tokenId: TokenId
  ): F[String] =
    getAsString(
      uri"$BASE_URL/snapshot/token/$tokenId"
    )

  def getStatusEndpoint: F[String] =
    getAsString(
      uri"$BASE_URL/status"
    )

  def getOwnershipSnapshot(
    tokenId: TokenId
  ): F[String] =
    getAsString(
      uri"$BASE_URL/snapshot/ownership/$tokenId"
    )

  def getOwnershipSnapshotMap(
    tokenId: Option[String],
    limit:   Option[String]
  ): F[String] =
    getAsString(
      uri"$BASE_URL/snapshot/ownership?from=$tokenId&limit=$limit"
    )

  def getOwnershipRewarded(
    tokenId: TokenId,
  ): F[String] =
    getAsString(
      uri"$BASE_URL/rewarded/ownership/$tokenId"
    )

  def getReward(
    tokenId: TokenId,
  ): F[String] =
    getAsString(
      uri"$BASE_URL/rewarded/ownership/$tokenId"
    )

  def getTx(
    txHash: String,
  ): F[String] =
    getAsString(
      uri"$BASE_URL/tx/$txHash"
    )

  def getTxSet(
    txHash: String,
  ): F[String] =
    getAsString(
      uri"$BASE_URL/tx/$txHash"
    )

  def postTx(
    txs: String,
  ): F[String] =
    postAsString(
      uri"$BASE_URL/tx", txs
    )

  def postTxHash(
    txs: String,
  ): F[String] =
    postAsString(
      uri"$BASE_URL/txHash", txs
    )
  

  def jobOrBlock: F[Unit] = 
    blocker.get.flatMap { value =>
      value match 
        case false => {
          Async[F].sleep(3.second) 
            >> jobOrBlock
        }
        case true  => Async[F].unit
    }
  
    
