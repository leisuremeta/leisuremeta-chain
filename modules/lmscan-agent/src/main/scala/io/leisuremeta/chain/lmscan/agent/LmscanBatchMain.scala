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
import io.leisuremeta.chain.lmscan.agent.entity.{Block,Tx, AccountEntity}
import io.leisuremeta.chain.lmscan.agent.model.{PBlock, BlockInfo, NodeStatus, NftMetaInfo}
import java.nio.file.Paths
import scala.util.Try
import java.nio.file.Files
import scala.jdk.CollectionConverters.*
// import io.leisuremeta.chain.lmscan.agent.service.TxService
import cats.syntax.traverse.toTraverseOps
import io.leisuremeta.chain.lmscan.agent.service.{BlockService, NftService}
import java.sql.Timestamp
import java.time.Instant
import io.leisuremeta.chain.lmscan.agent.repository.TxRepository
import io.leisuremeta.chain.lmscan.agent.entity.Nft
import io.leisuremeta.chain.lmscan.agent.entity.NftFile


import scala.concurrent.ExecutionContext
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import cats.implicits.*
import java.sql.SQLException

import api.model.*
import io.leisuremeta.chain.api.model.Transaction.*
import io.getquill.Insert
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*

import sttp.model.Uri
import io.getquill.Update


object LmscanBatchMain extends IOApp:
  
  val baseUri = "http://test.chain.leisuremeta.io"

  val ctx = new PostgresJAsyncContext(SnakeCase, "ctx")
  import ctx.{*, given}
  inline def upsertTransaction[F[_]: Async, T](
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

  inline def updateTransaction[F[_]: Async, T](
      inline query: Update[T]
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

  def genUpsertNftFileQuery(nft: MintNFT, metaInfo: NftMetaInfo): Insert[NftFile] =
    quote {
      query[NftFile]
        .insertValue(lift(NftFile(
          nft.tokenId.utf8.value,
          nft.tokenDefinitionId.utf8.value,
          metaInfo.Collection_name,
          metaInfo.NFT_name,
          metaInfo.NFT_URI,
          metaInfo.Creator_description,
          nft.dataUrl.value,
          metaInfo.Rarity,
          metaInfo.Creator,
          nft.createdAt.getEpochSecond(),
          Instant.now().getEpochSecond(),
          nft.output.utf8.value
        )))
        .onConflictUpdate(_.tokenId)((t, e) => t.tokenId -> e.tokenId)
    }

  def genUpsertNftQueryFromMintNft(nft: MintNFT, txHash: String): Insert[Nft] =
    quote {
      query[Nft]
        .insertValue(lift(Nft (
          nft.tokenId.utf8.value, 
          txHash, 
          Some(nft.rarity.utf8.value),
          nft.output.utf8.value,
          "MintNFT",
          None,
          nft.output.utf8.value,
          nft.createdAt.getEpochSecond(),
          Instant.now().getEpochSecond()
        )))
        .onConflictUpdate(_.tokenId)((t, e) => t.tokenId -> e.tokenId)
    }

  def genUpdateNftQueryFromTransferNFT(nft: TransferNFT): Update[Nft] =
    query[Nft]
      .updateValue(lift(Nft(
        nft.tokenId.utf8.value,
        nft.input.toUInt256Bytes.toHex, // ???
        None,
        nft.output.utf8.value,
        "TransferNft",
        None,
        nft.output.utf8.value,
        nft.createdAt.getEpochSecond(),
        Instant.now().getEpochSecond(),
      )))

  def genUpdateNftQueryFromBurnNFT(nft: BurnNFT, tokenId: String): Update[Nft] =
    query[Nft]
      .updateValue(lift(Nft(
        tokenId,
        nft.input.toUInt256Bytes.toHex, // ???
        None,
        nft.output.utf8.value,
        "BurnNFT",
        None,
        nft.output.utf8.value,
        nft.createdAt.getEpochSecond(),
        Instant.now().getEpochSecond(),
      )))
  
  def genUpdateNftQueryFromEntrustNFT(nft: EntrustNFT): Update[Nft] =
    query[Nft]
      .updateValue(lift(Nft(
        nft.tokenId.utf8.value,
        nft.input.toUInt256Bytes.toHex, // ???
        None,
        nft.to.utf8.value,
        "EntrustNFT",
        None,
        nft.to.utf8.value,
        nft.createdAt.getEpochSecond(),
        Instant.now().getEpochSecond(),
      )))
  
  def genUpdateNftQueryFromDisposeEntrustedNFT(nft: DisposeEntrustedNFT): Update[Nft] =
    query[Nft]
      .updateValue(lift(Nft(
        nft.tokenId.utf8.value,
        nft.input.toUInt256Bytes.toBytes.toHex, // ???  nft.input.toUInt256Bytes.toHex
        None,
        nft.to.utf8.value,
        "DisposeEntrustedNFT",
        None,
        nft.to.utf8.value,
        nft.createdAt.getEpochSecond(),
        Instant.now().getEpochSecond(),
      )))

  def genUpsertAccountQuery(tx: AccountEntity): Insert[AccountEntity] =
    query[AccountEntity]
      .insertValue(tx)
      .onConflictUpdate(_.address)((t, e) => t.address -> e.address)



  def run(args: List[String]): IO[ExitCode] =
    ArmeriaCatsBackend
      .resource[IO](SttpBackendOptions.Default)
      .use { backend =>
        for
          status <- getStatus[IO](backend)
          block  <- getBlock[IO](backend)(status.bestHash)
          // TODO: read sequentially saved last block
          lastBlockHash <- BlockService.getLastSavedBlock[IO].flatMap {
            case Some(lastBlock) => EitherT.pure(lastBlock.hash)
            case None => EitherT.pure(status.genesisHash)
          }
          count <- loop[IO](backend)(
            status.bestHash,
            // status.genesisHash,
            lastBlockHash,
          ) { (blockHash, block, txList) =>
            for 
              txs <- txList.traverse { txHash =>
                for
                  result <- getTransaciton[IO](backend)(txHash)
                  (txResult, txJson) = result

                  // TODO: NFT 타입 트랜잭션은 nft 테이블에 추가 저장. (extract nft entity field)
                  // extract nft entity from sub nft models
                  txEntity <- txResult.signedTx match 
                    case tx: Transaction.TokenTx => tx match 
                      case nft: MintNFT => {
                        val url = Uri.unsafeParse(nft.dataUrl.value)
                        val txEntity: EitherT[IO, String, Option[Tx]] = get[IO, NftMetaInfo](backend)(url).flatMap {
                          // metaInfo 받아오는거 실패하면 이후 로직 진행 불가 에러 로그 후 탈출.
                          case metaInfo: NftMetaInfo =>
                            upsertTransaction[IO, NftFile](genUpsertNftFileQuery(nft, metaInfo))
                            upsertTransaction[IO, Nft](genUpsertNftQueryFromMintNft(nft, txHash))
                            EitherT.pure(Some(Tx.fromNft(txHash, nft, block, blockHash, txJson)))
                        }
                        txEntity.recover{(msg: String) => scribe.info(s"get error"); None}
                      }  
                      case nft: TransferNFT => {
                        updateTransaction[IO, Nft](genUpdateNftQueryFromTransferNFT(nft))
                        Some(Tx.fromNft(txHash, nft, block, blockHash, txJson))
                      }
                      case nft: BurnNFT => {
                        val txHash = nft.input.toUInt256Bytes.toHex
                        val newTxEntity = for 
                            nftEntityInDb <- NftService.getByTxHash[IO](txHash)
                          yield nftEntityInDb.map {
                            prevNftEntity => 
                              updateTransaction[IO, Nft](genUpdateNftQueryFromBurnNFT(nft, prevNftEntity.tokenId))
                              Tx.fromNft(txHash, nft, prevNftEntity.tokenId, block, blockHash, txJson)
                          }
                        newTxEntity.recover{(msg: String) =>
                          scribe.info(s"previous nft by txHash '${txHash}' entity doesn't exist in db")
                          None
                        }
                      }
                      case nft: EntrustNFT => {
                        updateTransaction[IO, Nft](genUpdateNftQueryFromEntrustNFT(nft))
                        Tx.fromNft(txHash, nft, block, blockHash, txJson)
                      }
                      case nft: DisposeEntrustedNFT => {
                        updateTransaction[IO, Nft](genUpdateNftQueryFromDisposeEntrustedNFT(nft))
                        Tx.fromNft(txHash, nft, block, blockHash, txJson)
                      }
                    case tx: Transaction.AccountTx => tx match
                      case CreateAccount => {
                        upsertTransaction(IO, AccountEntity)(genUpsertAccountQuery(AccountEntity.from(tx)))
                        Tx.fromTx(tx)
                      }
                      case UpdateAccount => {

                      }
                      case AddPublicKeySummaries => ???
                      case AddPublicKeySummariesResult => ???
                      

                  v <- insertTransaction[IO, Tx](quote {
                    query[Tx]
                      .insertValue(lift(txEntity))
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
  )(next: String, lastSavedBlockHash: String)(
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
          for
            body <- response.body
            a    <- decode[A](body).leftMap(_.getMessage())
          yield a
        }
    }
  def getWithJson[F[_]: Async, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, (A, String)] =
    EitherT {
      basicRequest
        .get(uri)
        .send(backend)
        .map { response => 
          for
            body <- response.body
            a <- decode[A](body).leftMap(_.getMessage())  
          yield (a, body)
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
  )(txHash: String): EitherT[F, String, (TransactionWithResult, String)] =
    getWithJson[F, TransactionWithResult](backend) {
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
