package io.leisuremeta.chain

import cats.effect.IOApp
import cats.effect.IO
import cats.effect.ExitCode
import cats.data.EitherT
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.client3.SttpBackendOptions
import sttp.client3.*
import sttp.model.Uri

import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.refined.*
import io.circe.syntax.*

import scala.concurrent.duration.*
import cats.effect.kernel.Async
import cats.Monad
import cats.syntax.bifunctor.*
import cats.syntax.functor.*
import io.leisuremeta.chain.lmscan.agent.entity.Block
import io.leisuremeta.chain.lmscan.agent.model.{PBlock, BlockInfo, NodeStatus}
import java.nio.file.Paths
import scala.util.Try
import java.nio.file.Files
import scala.jdk.CollectionConverters.*
// import io.leisuremeta.chain.lmscan.agent.service.TxService
import cats.syntax.traverse.toTraverseOps
import io.leisuremeta.chain.lmscan.agent.entity.Tx
import io.leisuremeta.chain.lmscan.agent.service.BlockService
import java.sql.Timestamp
import java.time.Instant
import io.leisuremeta.chain.lmscan.agent.repository.TxRepository
import io.leisuremeta.chain.lmscan.backend.entity.Nft


import scala.concurrent.ExecutionContext
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import cats.implicits.*
import java.sql.SQLException

import api.model.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.getquill.Insert



object LmscanBatchMain extends IOApp:
  
  val baseUri = "http://test.chain.leisuremeta.io"

  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")
  import ctx.{*, given}
  inline def insertTransaction[F[_]: Async, T](
      inline query: Insert[T]
  ): EitherT[F, String, Long] =
    scribe.info("222222")
    EitherT {
      Async[F].recover {
        for
          given ExecutionContext <- Async[F].executionContext
          ids <- Async[F]
            .fromCompletableFuture(Async[F].delay {
              scribe.info("333333")
              ctx.transaction[Long] {
                for p <- ctx
                    .run(
                      // quote {
                      //   query[T]
                      //     .insertValue(
                      //       lift(entity),
                      //     )
                      //     .onConflictUpdate(_.id())((t, e) => t.id() -> e.id())
                      // },
                      query
                    )
                yield p
              }
            })
            .map(Either.right(_))
        yield
          scribe.info("444444")
          ids
      } {
        case e: SQLException =>
          scribe.info("55555")
          Left(s"sql exception occured: " + e.getMessage())
        case e: Exception =>
          scribe.info("66666: " + e.getMessage())
          Left(e.getMessage())
      }
    }

  def run(args: List[String]): IO[ExitCode] =
    ArmeriaCatsBackend
      .resource[IO](SttpBackendOptions.Default)
      .use { backend =>
        for
          status <- getStatus[IO](backend)
          // TODO: read sequentially saved last block
          block  <- getBlock[IO](backend)(status.bestHash)
          val lastBlockNumber = BlockService.getLastSavedBlock[IO].flatMap {
            case Some(lastBlock) => lastBlock.number
            case None => getBlock[IO](backend)(status.genesisHash)
          }
          count <- loop[IO](backend)(
            status.bestHash,
            status.genesisHash,
          ) { (blockHash, block, txList) =>
            for 
              txs <- txList.traverse { txHash =>
                for
                  txResult <- getTransaciton[IO](backend)(txHash)

                  // TODO: NFT 타입 트랜잭션은 nft 테이블에 추가 저장. (extract nft entity field)
                  // extract nft entity from sub nft models
                  nftEntity: Option[Nft] <- txResult.signedTx match 
                    case tx: Transaction.TokenTx => match 
                    // TODO: 
                    // case nft: DefineToken => Nft(nft)
                    case nft: MintNFT => {
                          insertTransaction[IO, NftFile]( NftFile() )
                          Nft()
                        }  
                    case nft: TransferNFT => Nft()
                    case nft: BurnNFT => Nft()
                    case nft: EntrustNFT => Nft()
                    case nft: DisposeEntrustedNFT => Nft()
                    // case nft: MintFungibleToken =>  
                    // case nft: TransferFungibleToken =>
                    // case nft: BurnFungibleToken =>  
                    // case nft: EntrustFungibleToken =>
                    // case nft: DisposeEntrustedFungibleToken =>  

                  //  _ <- nftEntity.map( nft => insertTransaction[IO, Nft](nft)) 

                  v <- insertTransaction[IO, Tx](quote {
                    query[Tx]
                      .insertValue(lift(txResult))
                      .onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash)
                  })
                
                yield ()
              }
              // _ <- TxService.insert[IO, Block]( Block (
              _ <- insertTransaction[IO, Block](
                quote {
                  query[Block]
                    .insertValue(
                      lift( Block (
                        blockHash,
                        block.header.number,
                        block.header.parentHash,
                        block.transactionHashes.size,
                        Timestamp.valueOf(block.header.timestamp).getTime(),
                        Instant.now().getEpochSecond(),
                      )),
                    )
                    .onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash)
                  },
                ) 
            yield ()
          }
        yield ()

        //   case None => EitherT.pure(())
        // }
      }
      .as(ExitCode.Success)

  def loop[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(next: String, genesis: String)(
      run: (String, PBlock, Seq[String]) => EitherT[F, String, Unit],
  ): EitherT[F, String, Long] =
    for
      block <- getBlock[F](backend)(next)
      _     <- EitherT.pure(scribe.info(s"block ${block.header.number}: $next"))
      _     <- run(next, block, block.transactionHashes)
    yield 2L

  // def loop[F[_]: Async](
  //     backend: SttpBackend[F, Any],
  // )(next: String, genesis: String, count: Long): EitherT[F, String, Long] =
  //   for
  //     block <- getBlock[F](backend)(next)
  //     _     <- EitherT.pure(scribe.info(s"block ${block.header.number}: $next"))
  //   yield 2L

  def checkLoop(): IO[Unit] = for
    _ <- IO.delay(scribe.info(s"data insertion started"))
    _ <- checkBlocks()
  yield ()

  def checkBlocks(): IO[Unit] = for _ <- IO.none // for preventing compile error
  yield ()

  def get[F[_]: Async, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, A] =
    EitherT {
      basicRequest
        .get(uri)
        .send(backend)
        .map { response =>
          val result = for
            body <- response.body
            a    <- decode[A](body).leftMap(_.getMessage())
          yield a

          result
        }
    }

  def getStatus[F[_]: Async](
      backend: SttpBackend[F, Any],
  ): EitherT[F, String, NodeStatus] =
    get[F, NodeStatus](backend) {
      uri"$baseUri/status"
    }

  def getTransaciton[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(txHash: String): EitherT[F, String, TransactionWithResult] =
    get[F, TransactionWithResult](backend) {
      uri"$baseUri/tx/${txHash}"
    }.leftMap { msg =>
      scribe.error(s"getTransaciton error msg: $msg")  
      msg
    }

  def getBlock[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(blockHash: String): EitherT[F, String, PBlock] =
    get[F, PBlock](backend) {
      uri"$baseUri/block/${blockHash}"
    }.leftMap { msg =>
      scribe.error(s"getBlock error msg: $msg")  
      msg
    }



  def readUnsavedBlocks(): IO[Seq[String]] = IO.blocking {
    val path = Paths.get("unsaved-blocks.json")
    val seqEither = for
      json <- Try(Files.readAllLines(path).asScala.mkString("\n")).toEither
      seq  <- decode[Seq[String]](json)
    yield seq
    seqEither match
      case Right(seq) => seq
      case Left(e) =>
        e.printStackTrace()
        scribe.error(s"Error reading unsaved blocks: ${e.getMessage}")
        Seq.empty
  }

  def getLastBlockRead(): IO[Option[Block]] =
    IO.blocking {
      ???
    }
