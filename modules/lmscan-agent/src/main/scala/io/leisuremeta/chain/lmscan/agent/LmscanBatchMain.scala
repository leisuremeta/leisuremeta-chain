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
import io.leisuremeta.chain.lmscan.agent.entity.{NftTxEntity, BlockEntity, TxEntity, AccountEntity, BlockSavedLog, BlockStateEntity, TxStateEntity}
import io.leisuremeta.chain.lmscan.agent.model.*
import java.nio.file.Paths
import scala.util.Try
import java.nio.file.Files
import scala.jdk.CollectionConverters.*
// import io.leisuremeta.chain.lmscan.agent.service.TxService
import cats.syntax.traverse.toTraverseOps
import io.leisuremeta.chain.lmscan.agent.service.{BlockService, NftService, StateService}
import java.sql.Timestamp
import java.time.Instant
import io.leisuremeta.chain.lmscan.agent.repository.TxRepository
import io.leisuremeta.chain.lmscan.agent.entity.NftFile


import scala.concurrent.ExecutionContext
import io.getquill.PostgresJAsyncContext
import io.getquill.SnakeCase
import io.getquill.*
import scala.concurrent.ExecutionContext.global
import scala.concurrent.ExecutionContext
import cats.implicits.*
import java.sql.SQLException

import io.getquill.Insert
import io.leisuremeta.chain.api.model.Block

// import api.model.*
// import api.model.Block
import io.leisuremeta.chain.api.model.Signed.Tx
import io.leisuremeta.chain.api.model.Transaction.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*

import sttp.model.Uri
import io.getquill.Update
import sttp.client3.SttpBackend
import io.leisuremeta.chain.api.model.NodeStatus
import io.leisuremeta.chain.api.model.TransactionWithResult

import api.model.TransactionWithResult.ops.toSignedTxHash
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import api.model.Block.ops.*
import api.model.TransactionWithResult.ops.*

import io.leisuremeta.chain.lib.crypto.Hash
import io.leisuremeta.chain.api.model.Transaction
import io.leisuremeta.chain.api.model.Signed




object LmscanBatchMain extends IOApp:
  
  val baseUri = "http://test.chain.leisuremeta.io"

  // for excluding auto-generated column named 'id'
  // given StateEntityInsertMeta = insertMeta[StateEntity](_.id)  

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
                for p <- ctx.run(query)
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
                for p <- ctx.run(query)
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
        .onConflictUpdate(_.tokenId)((t, e) => t.txHash -> e.txHash)
    }

  def genUpdateNftQueryFromTransferNFT(nft: TransferNFT): Update[NftTxEntity] =
//      query[Person].filter(p => p.id == lift(999)).update(_.age -> lift(18))

    query[NftTxEntity]
      .filter(n => n.tokenId == lift(nft.tokenId.utf8.value))
      .update()

  def genUpdateNftQueryFromBurnNFT(nft: BurnNFT, tokenId: String): Insert[NftTxEntity] =
    query[NftTxEntity]
      .insertValue(lift(NftTxEntity(
        tokenId,
        nft.input.toUInt256Bytes.toHex, // ???
        None,
        nft.output.utf8.value,
        "BurnNFT",
        None,
        nft.output.utf8.value,
        nft.creaedAt.getEpochSecond(),
        Instant.now().getEpochSecond(),
      )))
      .onConflictUpdate(_.tokenId)((t, e) => t.txHash -> e.txHash)
  
  def genUpdateNftQueryFromEntrustNFT(nft: EntrustNFT): Insert[NftTxEntity] =
    query[NftTxEntity]
      .insertValue(lift(NftTxEntity(
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
  
  def genUpdateNftQueryFromDisposeEntrustedNFT(nft: DisposeEntrustedNFT): Insert[NftTxEntity] =
    query[NftTxEntity]
      .insertValue(lift(NftTxEntity(
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
  
  def genBlockStateEntityInsertQuery(eventTime: Long, json: String): Insert[BlockStateEntity] =
    query[BlockStateEntity].insertValue(BlockStateEntity (
      id = 0L, // generated id
      eventTime = eventTime,
      json = json,
      isBuild = false
    ))

  def genTxStateEntityInsertQuery(eventTime: Long, blockHash: String, json: String): Insert[TxStateEntity] =
    query[TxStateEntity].insertValue(TxStateEntity (
      id = 0L, // generated id
      eventTime = eventTime,
      blockHash = blockHash,
      json = json,
      isBuild = false
    ))
  
  
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

  def getBlock[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(blockHash: Block.BlockHash): EitherT[F, String, (Block, String)] =
    getWithJson[F, Block](backend) {
      uri"$baseUri/block/${blockHash.toUInt256Bytes.toBytes.toHex}"
    }.leftMap { msg =>
      scribe.error(s"getBlock error msg: $msg")  
      msg
    }

  def getStatus[F[_]: Async](
      backend: SttpBackend[F, Any],
  ): EitherT[F, String, NodeStatus] =
    get[F, NodeStatus](backend) {
      uri"$baseUri/status"
    }
  
  
  
  def checkLoop[F[_]: Async](
    backend: SttpBackend[F, Any],
    config: BatchConfig,
  ): IO[Unit] = 
    // 트랜잭션 단위 = 블락 단위: 블락과 블락에 있는 트랜잭션들을 Transaction of unit 으로 봄.

    def bestBlock[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, (Block, String)] = 
      for status <- getStatus[F](backend)
      block <- getBlock[F](backend)(status.bestHash)
      yield block

    
    def lastSavedBlock[F[_]: Async]: EitherT[F, String, /*prevLastSavedBlock:*/ Option[BlockSavedLog]] = 
       BlockService.getLastSavedBlock[F]

    // loop from bestBlock to lastSavedBlock's next block 
    def saveDiffStateLoop[F[_]: Async](backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): EitherT[F, String, /* lastSavedBlock:*/ Option[Block]] = 
      def isContinue(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): Boolean =
        val result = for 
          currBlock <- currBlockOpt
          (block, json) = currBlock
        yield block.toHash.toUInt256Bytes.toBytes.toHex != lastSavedBlockHash
        result.getOrElse(false)

      def loop(backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): Option[(Block, String)] =
        if isContinue(currBlockOpt, lastSavedBlockHash) then
          val s = for 
            result <- currBlockOpt
            (block, blockJson) = result
            _ <- upsertTransaction[F, BlockStateEntity](genBlockStateEntityInsertQuery(block.header.timestamp.getEpochSecond(), blockJson))

            txs <- block.transactionHashes.toList.traverse { 
              (txHash: Hash.Value[Signed.Tx]) =>
                for
                  result <- getTransaciton[F](backend)(txHash.toUInt256Bytes.toBytes.toHex)
                  (txResult, txJson) = result
                  
                  _ <- upsertTransaction[F, TxStateEntity](genTxStateEntityInsertQuery(txResult.signedTx.value.createdAt.getEpochSecond(), block.toHash.toUInt256Bytes.toBytes.toHex, txJson))
                yield ()
              }
            nextBlock <- getBlock[F](backend)(block.header.parentHash)
          // block.header.parentHash.toUInt256Bytes.toHex
          yield nextBlock
          loop(backend)(s, lastSavedBlockHash)
        else 
          None
        
      loop(currBlockOpt, lastSavedBlockHash)

      // EitherT.cond (
      //   isContinue(currBlockOpt, lastSavedBlockHash),

      //   for 
      //     result <- currBlockOpt
      //     (block, blockJson) = result
      //     _ <- upsertTransaction[F, BlockStateEntity](genBlockStateEntityInsertQuery(block.header.timestamp.getEpochSecond(), blockJson))

      //     txs <- block.transactionHashes.toList.traverse { 
      //       (txHash: Hash.Value[Signed.Tx]) =>
      //         for
      //           result <- getTransaciton[F](backend)(txHash.toUInt256Bytes.toBytes.toHex)
      //           (txResult, txJson) = result
                
      //           _ <- upsertTransaction[F, TxStateEntity](genTxStateEntityInsertQuery(txResult.signedTx.value.createdAt.getEpochSecond(), block.toHash.toUInt256Bytes.toBytes.toHex, txJson))
      //         yield ()
      //       }
      //     nextBlock <- getBlock[F](backend)(block.header.parentHash)
      //   // block.header.parentHash.toUInt256Bytes.toHex
      //   yield saveDiffStateLoop(backend)(Some(nextBlock), lastSavedBlockHash),

      //   "error occured"
      // )
      
    def isEqual(prevLastSavedBlockHash: String, parentHashOfCurrLastSavedBlock: String): IO[Boolean] = 
      IO.blocking { prevLastSavedBlockHash == parentHashOfCurrLastSavedBlock }
    
    // isEqual  match
    //   case true =>
    def buildSavedStateLoop[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, /*currLastSavedBlock:*/ Unit] = 
      for 
        blockStates <- StateService.getBlockStatesByNotBuildedOrderByEventTimeAsc[F]
        _ <- blockStates.map(b => decode[Block](b.json)).traverse { blockEither => 
          for 
            block: Block <- blockEither
            blockHash = block.toHash.toUInt256Bytes.toBytes.toHex
            txStates <- StateService.getTxStatesByBlockAndNotBuildedOrderByEventTimeAsc[F](blockHash)
            txWithResults <- txStates.traverse { txState => decode[TransactionWithResult](txState.json)}
            _ <- (txWithResults zip txStates.map(_.json)).traverse {
              case (txResult, txJson) =>
                val txHash = txResult.signedTx.toHash.toUInt256Bytes.toBytes.toHex
                for
                  _ <- txResult.signedTx match 
                    case tokenTx: Transaction.TokenTx => tokenTx match 
                      case nft: MintNFT => {
                        val url = Uri.unsafeParse(nft.dataUrl.value)
                        val txEntity: EitherT[F, String, Option[TxEntity]] = get[F, NftMetaInfo](backend)(url).flatMap {
                          // metaInfo 받아오는거 실패하면 이후 로직 진행 불가 에러 로그 후 탈출.
                          case metaInfo: NftMetaInfo =>
                            upsertTransaction[F, NftTxEntity](genUpsertNftQueryFromMintNft(nft, txHash))
                            upsertTransaction[F, NftFile](genUpsertNftFileQuery(nft, metaInfo))
                            EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                        }
                        txEntity.recover{(msg: String) => scribe.info(s"get error"); None}
                      }  
                      case nft: TransferNFT => {
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(NftTxEntity.from(nft)).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        updateTransaction[F, NftFile](
                          query[NftFile].filter(n => n.tokenId == lift(nft.tokenId.utf8.value)).update(_.owner -> nft.output.utf8.value)
                        )
                        Some(TxEntity.from(txHash, nft, block, blockHash, txJson))
                      }
                      case nft: BurnNFT => {
                        val txHash = nft.input.toUInt256Bytes.toHex
                        val newTxEntity = for 
                            // TODO: burnNft 트랜잭션에서는 tokenId가 없어서 이전 트랜잭션 데이터들에서 tokenId 값을 가져와야 하는데 조회에 사용할 필드가 없음.
                            nftEntityInDb <- NftService.getByTxHash[F](txHash)
                          yield nftEntityInDb.map {
                            prevNftEntity => 
                              upsertTransaction[F, NftTxEntity](
                                query[NftTxEntity].insertValue(NftTxEntity.from(nft, prevNftEntity.tokenId)).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                              )
                              TxEntity.from(txHash, nft, prevNftEntity.tokenId, block, blockHash, txJson)
                          }
                        newTxEntity.recover{(msg: String) =>
                          scribe.info(s"previous nft by txHash: ${txHash} entity doesn't exist in db")
                          None
                        }
                      }
                      case nft: EntrustNFT => {
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(NftTxEntity.from(nft)).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        TxEntity.from(txHash, nft, block, blockHash, txJson)
                      }
                      case nft: DisposeEntrustedNFT => {
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(NftTxEntity.from(nft)).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        TxEntity.from(txHash, nft, block, blockHash, txJson)
                      }
                    case accountTx: Transaction.AccountTx => accountTx match
                      case tx: CreateAccount => {
                        upsertTransaction[F, AccountEntity](
                          query[AccountEntity].insertValue(AccountEntity.from(tx)).onConflictUpdate(_.address)((t, e) => t.address -> e.address)
                        )
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: UpdateAccount => {
                        // updateTransaction[F, AccountEntity](genUpdateAccountQuery(AccountEntity.from(tx)))
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: AddPublicKeySummaries => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                    case groupTx: Transaction.GroupTx => groupTx match
                      case tx: CreateGroup => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: AddAccounts => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                    case rewardTx: Transaction.RewardTx => rewardTx match
                      case tx: RegisterDao => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: UpdateDao => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: RecordActivity => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: BuildSnapshot => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: ExecuteAccountReward => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: ExecuteTokenReward => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                      case tx: ExecuteOwnershipReward => {
                        TxEntity.from(txHash, tx, block, blockHash, txJson)
                      }
                yield ()
            }
          yield ()
        }        
      yield ()

    // isEqual  match
    //   case true =>
      //     .insert(_.id -> 1, _.sku -> 10)
    def saveLastSavedBlockLog[F[_]: Async](blockJson: (Block, String)) = EitherT[F, String, BlockStateEntity] = 
      (json, block) = blockJson
      upsertTransaction[F, BlockSavedLog](
        query[BlockSavedLog]
          .insert(
            _.eventTime -> block.header.timestamp.getEpochSecond(),
            _.json -> json,
            _.isBuild -> false,
          )
        )

    //   case false =>
    // def rollbackSavedDiffStates(): IO[Unit] = ???


  
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

  

  def getTransaciton[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(txHash: String): EitherT[F, String, (TransactionWithResult, String)] =
    getWithJson[F, TransactionWithResult](backend) {
      uri"$baseUri/tx/${txHash}"
    }.leftMap { msg =>
      scribe.error(s"getTransaciton error msg: $msg")  
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
