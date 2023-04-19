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
import cats.effect.Ref
import cats.effect.kernel.Async
import cats.syntax.all._
import cats.syntax.functor._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import sttp.model.MediaType
import sttp.tapir.stringBody
import io.leisuremeta.chain.node.proxy.model.TxModel
import io.circe.generic.auto._

object InternalApiService:
  def apply[F[_]: Async](
    backend: SttpBackend[F, Any],
    blocker: Ref[F, Boolean],
    baseUrlLock: Ref[F, List[String]]
  ): F[InternalApiService[F]] =
    Async[F].delay(new InternalApiService[F](backend, blocker, baseUrlLock))

class InternalApiService[F[_]: Async](
  backend:      SttpBackend[F, Any],
  blocker:      Ref[F, Boolean],
  baseUrlsLock: Ref[F, List[String]]
):
  // val baseUrl = "http://lmc.leisuremeta.io"
  // val baseUrl = "http://test.chain.leisuremeta.io"
  
  // def backend[F[_]: Async]: SttpBackend[F, Any] = 
  //   ArmeriaCatsBackend.usingClient[F](webClient(SttpBackendOptions.Default))
  
  def getAsString(
    uri: Uri 
  ): F[String] = 
    for {
      _      <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .get(uri)
        .send(backend)
        .map(_.body)
    } yield 
      scribe.info(s"getAsString result: $result")
      result

  def getAsResponse(
    uri: Uri 
  ): F[Response[String]] = 
    for {
      _      <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .get(uri)
        .send(backend)
    } yield 
      scribe.info(s"getAsString result: $result")
      result
  


  def getTxAsResponse[A: io.circe.Decoder](
    uri: Uri 
  ): F[Either[String, A]] = 
    for {
      _      <- jobOrBlock
      result <- basicRequest
        .get(uri)
        .send(backend)
        .map { response =>
          for 
            body <- response.body
            _    <- Either.right(scribe.info(s"zzzz- $body"))
            // a    <- decode[A](body).leftMap(_.getMessage())
            a <- decode[A](body).leftMap { error =>
              val errorMessage = s"Decoding failed with error: $error"
              scribe.error(errorMessage)
              errorMessage
            }
          // _    <- Either.right(scribe.info(s"ssss- $a"))
          yield a
        }
        // .contentType(MediaType.ApplicationJson)
        // .out(stringBody)
        // .out(header("Content-Type", jsonType))
    } yield 
      scribe.info(s"getAsString result: $result")
      result

  def postAsString(
    uri: Uri,
    body: String,
  ): F[String] =
    for
      _      <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .post(uri)
        .body(body)
        .send(backend)
        .map(_.body)
    yield result

  def postAsResponse(
    uri: Uri,
    body: String,
  ): F[Response[String]] =
    for
      _      <- jobOrBlock
      result <- basicRequest
        .response(asStringAlways)
        .post(uri)
        .contentType(MediaType.ApplicationJson)
        .body(body)
        .send(backend)
        // .map(_.body)
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
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/block/$blockHash")
      }
    }.map(_.head)


  def getAccount(
    account: Account
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/account/${account.utf8.value}") 
      }
    }.map(_.head)
    
  def getEthAccount(
    ethAddress: EthAddress
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/eth/$ethAddress")
      }
    }.map(_.head)
  
  def getGroupInfo(
    groupId: GroupId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/group/$groupId")
      }
    }.map(_.head)


  def getBlockList(
    fromOption: Option[String],
    limitOption: Option[String],
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/block?from=${fromOption.getOrElse("")}&limit=${limitOption.getOrElse("")}")
      }
    }.map(_.head)

  def getStatus: F[String] =
    baseUrlsLock.get.flatMap { urls =>
      println(s"baseUrl---")
      urls.traverse { baseUrl =>
        println(s"baseUrl: ${baseUrl}")
        getAsString(uri"$baseUrl/status")
      }
    }.map(_.head)

  def getTokenDef(
    tokenDefinitionId: TokenDefinitionId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/token-def/$tokenDefinitionId")
      }
    }.map(_.head)

  def getBalance(
    account: Account,
    movable: String
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/balance/$account?movable=${movable}")
      }
    }.map(_.head)

  def getNftBalance(
    account: Account,
    movable: Option[String]
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/nft-balance/$account?movable=${movable}")
      }
    }.map(_.head)


  def getToken(
    tokenId: TokenId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/token/$tokenId")
      }
    }.map(_.head)

  def getOwners(
    tokenDefinitionId: TokenDefinitionId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/owners/$tokenDefinitionId")
      }
    }.map(_.head)

  def getAccountActivity(
    account: Account
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/activity/account/$account")
      }
    }.map(_.head)

  def getTokenActivity(
    tokenId: TokenId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/activity/token/$tokenId")
      }
    }.map(_.head)
      
    

  def getAccountSnapshot(
    account: Account
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/snapshot/account/$account")
      }
    }.map(_.head)
    

  def getTokenSnapshot(
    tokenId: TokenId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/snapshot/token/$tokenId")
      }
    }.map(_.head)

  def getStatusEndpoint: F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/status")
      }
    }.map(_.head)

  def getOwnershipSnapshot(
    tokenId: TokenId
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/snapshot/ownership/$tokenId")
      }
    }.map(_.head)

  def getOwnershipSnapshotMap(
    tokenId: Option[String],
    limit:   Option[String]
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/snapshot/ownership?from=$tokenId&limit=$limit")
      }
    }.map(_.head)

  def getOwnershipRewarded(
    tokenId: TokenId,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/rewarded/ownership/$tokenId")
      }
    }.map(_.head)

  def getReward(
    tokenId: TokenId,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/rewarded/ownership/$tokenId")
      }
    }.map(_.head)

  def getTx(
    txHash: String,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/tx/$txHash")
      }
    }.map(_.head)

  def getTxFromOld(
    txHash: String,
  ): F[Either[String, TxModel]] =
    baseUrlsLock.get.map(_.head).flatMap { baseUrl =>
      getTxAsResponse(uri"$baseUrl/tx/$txHash")(TxModel.txModelDecoder)
    }

  def getTxSet(
    txHash: String,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        getAsString(uri"$baseUrl/tx/$txHash")
      }
    }.map(_.head)

  def postTx(
    txs: String,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        postAsString(
          uri"$baseUrl/tx", txs
        )
      }
    }.map(_.head)

  def postTx(
    baseUrl: String,
    txs: String,
  ): F[Response[String]] =    
    postAsResponse(
      uri"$baseUrl/tx", txs
    )
    
  def postTxHash(
    txs: String,
  ): F[String] =
    baseUrlsLock.get.flatMap { urls =>
      urls.traverse { baseUrl =>
        postAsString(
          uri"$baseUrl/txHash", txs
        )    
      }
    }.map(_.head)

  def jobOrBlock: F[Unit] = 
    blocker.get.flatMap { value =>
      value match 
        case false => {
          Async[F].sleep(3.second) 
            >> jobOrBlock
        }
        case true  => Async[F].unit
    }
  
  def bestBlock(baseUri: String): F[Block] = 
    for 
      nodeStatus <- get[NodeStatus](uri"$baseUri/status")
      bestBlock  <- block(baseUri, nodeStatus.bestHash.toUInt256Bytes.toHex)
    yield bestBlock

  def block(baseUri: String, blockHash: String): F[Block] =
    get[Block](uri"$baseUri/block/$blockHash")

  def postTxs(baseUri: String, body: String): F[String] =
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

  def get[A: io.circe.Decoder](uri: Uri): F[A] =
    basicRequest
      .get(uri)
      .send(backend)
      .flatMap { response =>
        response.body match 
          case Right(body) => 
            decode[A](body) match 
            case Right(a) => Async[F].pure(a)
            case Left(error) => Async[F].raiseError(error)
          case Left(error) => Async[F].raiseError(new Exception(s"Error in response body: $error"))
      }
