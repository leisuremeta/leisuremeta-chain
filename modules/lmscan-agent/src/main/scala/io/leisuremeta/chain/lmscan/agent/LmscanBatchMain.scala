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
import cats.effect.{Async, Concurrent}
import cats.Monad
import cats.syntax.bifunctor.*
import cats.syntax.functor.*
import io.leisuremeta.chain.lmscan.agent.entity.{NftTxEntity, BlockEntity, TxEntity, AccountEntity, BlockSavedLog, BlockStateEntity, TxStateEntity}
import io.leisuremeta.chain.lmscan.agent.model.*
import java.nio.file.Paths
import scala.util.Try
import java.nio.file.Files
import scala.jdk.CollectionConverters.*
import cats.syntax.traverse.toTraverseOps
import io.leisuremeta.chain.lmscan.agent.service.{BlockService, NftService, StateService}
import java.sql.Timestamp
import java.time.Instant
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

// import api.model.*
// import api.model.Block
import io.leisuremeta.chain.api.model.Block
import io.leisuremeta.chain.api.model.Signed
import io.leisuremeta.chain.api.model.Signed.Tx
import io.leisuremeta.chain.api.model.Transaction
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

  inline def genUpsertNftFileQuery(nft: MintNFT, metaInfo: NftMetaInfo): Insert[NftFile] =
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

  inline def genUpsertAccountQuery(tx: AccountEntity): Insert[AccountEntity] =
    query[AccountEntity]
      .insertValue(lift(tx))
      .onConflictUpdate(_.address)((t, e) => t.address -> e.address)
  
  inline def genBlockStateEntityInsertQuery(eventTime: Long, json: String): Insert[BlockStateEntity] =
    query[BlockStateEntity].insert(
      _.eventTime -> lift(eventTime),
      _.json -> lift(json),
      _.isBuild -> lift(false)
    )

  inline def genTxStateEntityInsertQuery(eventTime: Long, blockHash: String, json: String): Insert[TxStateEntity] =
    query[TxStateEntity].insertValue(TxStateEntity (
      id = 0L, // generated id
      eventTime = lift(eventTime),
      blockHash = lift(blockHash),
      json = lift(json),
      isBuild = lift(false)
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
  )(blockHash: Block.BlockHash): EitherT[F, String, Option[(Block, String)]] =
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
  ): EitherT[F, String, Unit] = 
     // 트랜잭션 단위 = 블락 단위: 블락과 블락에 있는 트랜잭션들을 Transaction of unit 으로 봄.
    def bestBlock[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Option[(Block, String)]] = 
      for 
        status <- getStatus[F](backend)
        block <- getBlock[F](backend)(status.bestHash)
      yield block

    
    def getLastSavedBlock[F[_]: Async]: EitherT[F, String, /*prevLastSavedBlock:*/ Option[BlockSavedLog]] = 
       BlockService.getLastSavedBlock[F]

    //loop from bestBlock to lastSavedBlock's next block 
    def saveDiffStateLoop[F[_]: Async](backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String)
    : EitherT[F, String, /* lastSavedBlock:*/ Option[Block]] = 
      def isContinue(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): EitherT[F, String, (Block, String)] =
        currBlockOpt match
          case Some((block, json)) if block.toHash.toUInt256Bytes.toBytes.toHex != lastSavedBlockHash => EitherT.pure((block, json))
          case _ => EitherT.leftT(s"error ..")
      def loop1(backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): EitherT[F, String, Option[(Block, String)]] =
        for
          result1 <- isContinue(currBlockOpt, lastSavedBlockHash)
          (block, blockJson) = result1
          // _ <- upsertTransaction[F, BlockStateEntity](genBlockStateEntityInsertQuery(block.header.timestamp.getEpochSecond(), blockJson))
          _ <- upsertTransaction[F, BlockStateEntity](query[BlockStateEntity].insert(
              _.eventTime -> lift(block.header.timestamp.getEpochSecond()),
              _.json -> lift(blockJson),
              _.isBuild -> lift(false)
            ))
          
          txs <- block.transactionHashes.toList.traverse { 
            (txHash: Hash.Value[Signed.Tx]) =>
              for
                result <- getTransaciton[F](backend)(txHash.toUInt256Bytes.toBytes.toHex)
                _ = result.map { 
                  case (txResult, txJson) => 
                    upsertTransaction[F, TxStateEntity](genTxStateEntityInsertQuery(txResult.signedTx.value.createdAt.getEpochSecond(), block.toHash.toUInt256Bytes.toBytes.toHex, txJson))
                }
              yield ()
            }
        // block.header.parentHash.toUInt256Bytes.toHex
          nextBlockOpt <- getBlock[F](backend)(block.header.parentHash)
          result2 <- loop1(backend)(nextBlockOpt, lastSavedBlockHash)
        yield result2
        
      loop1(backend)(currBlockOpt, lastSavedBlockHash).map{ option => option.map(_._1)}

      // def loop(backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): Option[(Block, String)] =
      //   if isContinue(currBlockOpt, lastSavedBlockHash) then
      //     val s = for 
      //       result <- EitherT.fromOption(currBlockOpt, s"There is no current block: ${currBlockOpt}")
      //       (block, blockJson) = result
      //       _ <- upsertTransaction[F, BlockStateEntity](genBlockStateEntityInsertQuery(block.header.timestamp.getEpochSecond(), blockJson))

      //       txs <- block.transactionHashes.toList.traverse { 
      //         (txHash: Hash.Value[Signed.Tx]) =>
      //           for
      //             result <- getTransaciton[F](backend)(txHash.toUInt256Bytes.toBytes.toHex)
      //             (txResult, txJson) = result
                  
      //             _ <- upsertTransaction[F, TxStateEntity](genTxStateEntityInsertQuery(txResult.signedTx.value.createdAt.getEpochSecond(), block.toHash.toUInt256Bytes.toBytes.toHex, txJson))
      //           yield ()
      //         }
      //       nextBlock <- getBlock[F](backend)(block.header.parentHash)
      //     // block.header.parentHash.toUInt256Bytes.toHex
      //     yield nextBlock
      //     loop(backend)(s, lastSavedBlockHash)
      //   else 
      //     None
        


      
    def isEqual(prevLastSavedBlockHash: String, parentHashOfCurrLastSavedBlock: String): IO[Boolean] = 
      IO.blocking { prevLastSavedBlockHash == parentHashOfCurrLastSavedBlock }
    
    // isEqual  match
    //   case true =>
    def buildSavedStateLoop[F[_]: Async/*: cats.Applicative*/](backend: SttpBackend[F, Any]): EitherT[F, String, /*currLastSavedBlock:*/ Block] = 
      for 
        blockStates <- StateService.getBlockStatesByNotBuildedOrderByEventTimeAsc[F]
        blockWithJsonEither = blockStates.map(b => decode[Block](b.json))
        _ <-  blockWithJsonEither.traverse[EitherT[F, String, *], Unit] { blockEither => 
          for 
            block: Block <- EitherT.fromEither[F](blockEither).leftMap{ e => e.getMessage() }
            blockHash = block.toHash.toUInt256Bytes.toBytes.toHex
            txStates <- StateService.getTxStatesByBlockAndNotBuildedOrderByEventTimeAsc[F](blockHash)
            txWithResults <- txStates.traverse { txState => EitherT.fromEither[F](decode[TransactionWithResult](txState.json)).leftMap{ e => e.getMessage() } }
            _ <- (txWithResults zip txStates.map(_.json)).traverse[EitherT[F, String, *], Unit] {
              case (txResult, txJson) =>
                val txHash = txResult.signedTx.toHash.toUInt256Bytes.toBytes.toHex
                val fromAccount = txResult.signedTx.sig.account.utf8.value
                val result: EitherT[F, String, Option[TxEntity]] =  for 
                  txEntityOpt: Option[TxEntity] <- txResult.signedTx.value match 
                    case tokenTx: Transaction.TokenTx => tokenTx match 
                      case nft: DefineToken => {
                        EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                      }
                      case nft: MintNFT => {
                        val url = Uri.unsafeParse(nft.dataUrl.value)
                        val txEntity: EitherT[F, String, Option[TxEntity]] = get[F, NftMetaInfo](backend)(url).flatMap {
                          // metaInfo 받아오는거 실패하면 이후 로직 진행 불가 에러 로그 후 탈출.
                          case metaInfo: NftMetaInfo =>
                            upsertTransaction[F, NftTxEntity](
                              query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                            )
                            upsertTransaction[F, NftFile](quote {
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
                            })
                            EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                        }
                        txEntity.recover{(msg: String) => scribe.info(s"get error"); None}
                      }  
                      case nft: TransferNFT => {
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        updateTransaction[F, NftFile](
                          query[NftFile].filter(n => n.tokenId == lift(nft.tokenId.utf8.value)).update(_.owner -> lift(nft.output.utf8.value))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                      }
                      case nft: BurnNFT => {
                        val txHash = nft.input.toUInt256Bytes.toHex
                        val newTxEntity: EitherT[F, String, Option[TxEntity]] = 
                          for 
                            // TODO: burnNft 트랜잭션에서는 tokenId가 없어서 이전 트랜잭션 데이터들에서 tokenId 값을 가져와야 하는데 조회에 사용할 필드가 없음.
                            nftEntityInDb <- NftService.getByTxHash[F](txHash)
                          yield nftEntityInDb.map {
                            prevNftEntity => 
                              upsertTransaction[F, NftTxEntity](
                                query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, prevNftEntity.tokenId, txHash))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
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
                          query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                      }
                      case nft: DisposeEntrustedNFT => {
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson)))
                      }
                      
                      case tx: MintFungibleToken => {
                        var sum = 0l;
                        tx.outputs.toList.traverse { case (account, amount) =>
                          sum = sum + amount.toBigInt.longValue
                          updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                          )
                        }
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: TransferFungibleToken => {
                        var sum = 0l;
                        tx.outputs.toList.traverse { case (account, amount) =>
                          sum = sum + amount.toBigInt.longValue
                          updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                          )
                        }
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      }
                      case tx: EntrustFungibleToken => {
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(tx.to.utf8.value)).update(a => a.balance -> (a.balance + lift(tx.amount.toBigInt.longValue)))
                        )
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(tx.amount.toBigInt.longValue)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson,  fromAccount)))
                      }
                      case tx: DisposeEntrustedFungibleToken => {
                        var sum = 0l;
                        tx.outputs.toList.traverse { case (account, amount) =>
                          sum = sum + amount.toBigInt.longValue
                          updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                          )
                        }
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      }
                      case tx: BurnFungibleToken => {
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(tx.amount.toBigInt.longValue)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      }
                    case accountTx: Transaction.AccountTx => accountTx match
                      case tx: CreateAccount => {
                        upsertTransaction[F, AccountEntity](
                          query[AccountEntity].insertValue(lift(AccountEntity.from(tx))).onConflictUpdate(_.address)((t, e) => t.address -> e.address)
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: UpdateAccount => {
                        // updateTransaction[F, AccountEntity](genUpdateAccountQuery(AccountEntity.from(tx)))

                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: AddPublicKeySummaries => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                    case groupTx: Transaction.GroupTx => groupTx match
                      case tx: CreateGroup => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: AddAccounts => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                    case rewardTx: Transaction.RewardTx => rewardTx match
                      case tx: RegisterDao => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: UpdateDao => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: RecordActivity => {
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson)))
                      }
                      case tx: OfferReward => {
                        var sum = 0l;
                        tx.outputs.toList.traverse { case (account, amount) =>
                          sum = sum + amount.toBigInt.longValue
                          updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                          )
                        }
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                        )
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      }
                      case tx: ExecuteReward => {
                        txResult.result.getOrElse(EitherT.pure(None)) match {
                          case rewardRes: ExecuteRewardResult => 
                            var sum = 0l
                            rewardRes.outputs.toList.traverse { case (account, amount) =>
                              sum = sum + amount.toBigInt.longValue
                              updateTransaction[F, AccountEntity](
                                query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                              )
                            }
                            updateTransaction[F, AccountEntity](
                              query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                            )
                            EitherT.pure(Some(TxEntity.from(txHash, tx, rewardRes, block, blockHash, txJson, fromAccount)))
                          case _ => EitherT.pure(None)
                        }
                      }

                  _ = txEntityOpt match  
                    case Some(value) => upsertTransaction[F, TxEntity](query[TxEntity].insertValue(lift(value)))
                    case None => None

                yield txEntityOpt
                result.map(_=>())
            } 
          yield ()
        }
        blockWithJson <- blockWithJsonEither.traverse{ EitherT.fromEither(_) }.leftMap(e => e.getMessage())
      yield blockWithJson.last

    // isEqual  match
    //   case true =>
      //     .insert(_.id -> 1, _.sku -> 10)
    def saveLastSavedBlockLog[F[_]: Async](block: Block): EitherT[F, String, Long] =
      upsertTransaction[F, BlockSavedLog](
        query[BlockSavedLog]
          .insert(
            _.eventTime -> lift(block.header.timestamp.getEpochSecond()),
            _.hash -> lift(block.toHash.toUInt256Bytes.toBytes.toHex),
            _.json -> lift(block.asJson.spaces2),
            _.eventTime -> lift(block.header.timestamp.getEpochSecond()),
            _.createdAt -> lift(java.time.Instant.now().getEpochSecond()),
          )
        )
      

    //   case false =>
    // def rollbackSavedDiffStates(): IO[Unit] = ???

    def loop[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Unit] =
      for 
        _ <- EitherT.right(Async[F].delay(scribe.info(s"Checking for newly created blocks")))
        status <- getStatus[F](backend)
        _ <- getBlock[F](backend)(status.bestHash).map { value =>
          for 
            // TODO: read sequentially saved last block
            prevLastBlockHash: String <- getLastSavedBlock[F].map {
              case Some(lastBlock) => lastBlock.hash
              case None => status.genesisHash.toUInt256Bytes.toBytes.toHex
            }

            lastSavedBlockOpt <- saveDiffStateLoop[F](backend)(value, prevLastBlockHash)

            currLastSavedBlock <- buildSavedStateLoop[F](backend)

            _ <- saveLastSavedBlockLog[F]((currLastSavedBlock))
            
            _ <- EitherT.right(Async[F].delay(scribe.info(s"New block checking finished.")))
            _ <- EitherT.right(Async[F].sleep(10000.millis))
          yield ()
        }
        
        _ <- loop[F](backend)
      yield ()

    loop(backend)


  
  def getWithJson[F[_]: Async, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, Option[(A, String)]] =
    EitherT {
      basicRequest
        .get(uri)
        .send(backend)
        .map { response => 
          for
            body <- response.body
            a <- decode[A](body).leftMap(_.getMessage())  
          yield Some(a, body)
        }
    }

  

  def getTransaciton[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(txHash: String): EitherT[F, String, Option[(TransactionWithResult, String)]] =
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


  def run(args: List[String]): IO[ExitCode] =
    ArmeriaCatsBackend
      .resource[IO](SttpBackendOptions.Default)
      .use { backend =>
        val program = for
          _ <- checkLoop(backend)
        yield ()
        program.value
    }
    .as(ExitCode.Success)
