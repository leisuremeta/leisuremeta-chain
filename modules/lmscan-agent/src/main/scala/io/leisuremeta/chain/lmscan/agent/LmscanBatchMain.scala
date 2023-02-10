package io.leisuremeta.chain



import java.nio.file.{Files, Paths}
import java.sql.{SQLException, Timestamp}
import java.time.Instant

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.Try

import cats.{Monad, Parallel}
import cats.data.{EitherT, OptionT}
import cats.effect.{Async, Concurrent, ExitCode, IO, IOApp}
import cats.implicits.*
import cats.syntax.bifunctor.*
import cats.syntax.functor.*
import cats.syntax.traverse.toTraverseOps

import com.linecorp.armeria.client.{WebClient, ClientFactory}
import com.linecorp.armeria.client.encoding.DecodingClient

import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.refined.*
import io.circe.syntax.*
import io.getquill.{Insert, PostgresJAsyncContext, SchemaMeta, SnakeCase, Update, _}
import io.leisuremeta.chain.api.model.{Block, NodeStatus, Signed, Transaction, TransactionWithResult}
import io.leisuremeta.chain.api.model.Signed.Tx
import io.leisuremeta.chain.api.model.Transaction.*
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.lib.crypto.Hash
import io.leisuremeta.chain.lmscan.agent.entity.{NftFile, _}
import io.leisuremeta.chain.lmscan.agent.model.*
import io.leisuremeta.chain.lmscan.agent.service.*
import sttp.client3.{SttpBackend, SttpBackendOptions, _}
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.model.Uri

import api.model.TransactionWithResult.ops.toSignedTxHash
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import api.model.Block.ops.*
import api.model.TransactionWithResult.ops.*



object LmscanBatchMain extends IOApp:

  inline given SchemaMeta[BlockStateEntity] = schemaMeta[BlockStateEntity]("block_state")  
  inline given SchemaMeta[TxStateEntity] = schemaMeta[TxStateEntity]("tx_state")    
  inline given SchemaMeta[TxEntity] = schemaMeta[TxEntity]("tx")

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
  
  var testCount = 0;

  def blockCheckLoop[F[_]: Async](
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
          case Some((block, json)) => EitherT.pure((block, json))
          case _ => EitherT.leftT(s"there is no exist next block")

      def loop(backend: SttpBackend[F, Any])(currBlockOpt: Option[(Block, String)], lastSavedBlockHash: String): EitherT[F, String, Option[(Block, String)]] =
        inline given SchemaMeta[BlockStateEntity] = schemaMeta[BlockStateEntity]("block_state")  
        inline given SchemaMeta[TxStateEntity] = schemaMeta[TxStateEntity]("tx_state")  
        testCount = testCount+1
        
        for
          result1 <- isContinue(currBlockOpt, lastSavedBlockHash)
          (block, blockJson) = result1
          blockHash = block.toHash.toUInt256Bytes.toBytes.toHex
          blockstate <- upsertTransaction[F, BlockStateEntity](query[BlockStateEntity].insert(
              _.eventTime -> lift(block.header.timestamp.getEpochSecond()),
              _.number -> lift(block.header.number.toBigInt.longValue),
              _.hash -> lift(blockHash),
              _.json -> lift(blockJson),
              _.isBuild -> lift(false)
            ).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash))
          _ <- EitherT.right(Async[F].delay(scribe.info(s"blockstate upsertTransaction: $blockstate")))
          
          txs <- block.transactionHashes.toList.traverse { 
            (hash: Hash.Value[Signed.Tx]) => getTransaciton[F](backend)(hash.toUInt256Bytes.toBytes.toHex) }

          _ <- txs.traverse {
            case Some((txResult, txJson)) => 
              for 
                _ <- upsertTransaction[F, TxStateEntity](
                  query[TxStateEntity].insert(
                      _.hash -> lift(txResult.signedTx.toHash.toUInt256Bytes.toBytes.toHex),
                      _.eventTime -> lift(txResult.signedTx.value.createdAt.getEpochSecond()),
                      _.blockHash -> lift(blockHash),
                      _.json -> lift(txJson),
                    ).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash))
                 _ <- EitherT.right(Async[F].delay(scribe.info(s"txJson: $txJson")))
              yield ()

            case _ => EitherT.leftT(s"there is no exist transaction")  
          }
          nextBlockOpt <- getBlock[F](backend)(block.header.parentHash)
          

          result2 <- if blockHash != lastSavedBlockHash then loop(backend)(nextBlockOpt, lastSavedBlockHash) else loop(backend)(None, lastSavedBlockHash)
          // result2 <- if testCount < 10 then loop(backend)(nextBlockOpt, lastSavedBlockHash) else loop(backend)(None, lastSavedBlockHash)
          testCount = 0
        yield result2
        
      loop(backend)(currBlockOpt, lastSavedBlockHash).map{ option => option.map(_._1)}.recover((errMsg: String) => {
        println("saveDiffStateLoop is ended"); 
        None
      })
      
    def isEqual(prevLastSavedBlockHash: String, parentHashOfCurrLastSavedBlock: String): IO[Boolean] = 
      IO.blocking { prevLastSavedBlockHash == parentHashOfCurrLastSavedBlock }
    
    // isEqual  match
    //   case true =>
    def buildSavedStateLoop[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, /*currLastSavedBlock:*/ Option[Block]] = 
      inline given SchemaMeta[NftTxEntity] = schemaMeta[NftTxEntity]("nft")  
      inline given SchemaMeta[AccountEntity] = schemaMeta[AccountEntity]("account")  
      inline given SchemaMeta[BlockEntity] = schemaMeta[BlockEntity]("block")  

      for 
        blockStates <- StateService.getBlockStatesByNotBuildedOrderByEventTimeAsc[F]
        _ <- EitherT.right(Async[F].delay(scribe.info(s"getBlockStatesByNotBuildedOrderByEventTimeAsc: ${blockStates}")))

        blockWithJsonEither = blockStates.map(b => decode[Block](b.json))
        _ <- EitherT.right(Async[F].delay(scribe.info(s"aaaaa")))
        _ <-  blockWithJsonEither.traverse[EitherT[F, String, *], Unit] { blockEither => 
          for 
            block: Block <- EitherT.fromEither[F](blockEither).leftMap{ e => e.getMessage() }
            _ <- EitherT.right(Async[F].delay(scribe.info(s"block: ${block}")))
            blockHash = block.toHash.toUInt256Bytes.toBytes.toHex
            txStates <- StateService.getTxStatesByBlockOrderByEventTimeAsc[F](blockHash)
            txWithResults <- txStates.traverse { txState => EitherT.fromEither[F](decode[TransactionWithResult](txState.json)).leftMap{ e => e.getMessage() } }
            _ <- (txWithResults zip txStates.map(_.json)).traverse[EitherT[F, String, *], Unit] {
              case (txResult, txJson) =>
                scribe.info(s"(txResult, txJson): $txResult, $txJson")
                val txHash = txResult.signedTx.toHash.toUInt256Bytes.toBytes.toHex
                val fromAccount = txResult.signedTx.sig.account.utf8.value
                val result: EitherT[F, String, Option[TxEntity]] = for 
                  txEntityOpt: Option[TxEntity] <- txResult.signedTx.value match 
                    case tokenTx: Transaction.TokenTx => tokenTx match 
                      case nft: DefineToken => 
                        EitherT.pure(Some(TxEntity.from(txHash, nft, block, blockHash, txJson, fromAccount)))
                      case nft: MintNFT => 
                        val url = Uri.unsafeParse(nft.dataUrl.value)
                        val txEntity: EitherT[F, String, Option[TxEntity]] = get[F, NftMetaInfo](backend)(url).flatMap {
                          // metaInfo 받아오는거 실패하면 이후 로직 진행 불가 에러 로그 후 탈출.
                          case metaInfo: NftMetaInfo =>
                            println(s"metaInfo: ${metaInfo}")
                            for 
                              _ <- upsertTransaction[F, NftTxEntity](
                                query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash, fromAccount))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                              )
                              _ <- upsertTransaction[F, NftFile](quote {
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
                            yield Some(TxEntity.from(txHash, nft, block, blockHash, txJson, fromAccount))
                        }
                        txEntity.recover{(msg: String) => scribe.info(s"get error"); None}
                      case nft: TransferNFT => 
                        for
                          _ <- upsertTransaction[F, NftTxEntity](
                            query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash, fromAccount))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                          )
                          _ <- updateTransaction[F, NftFile](
                            query[NftFile].filter(n => n.tokenId == lift(nft.tokenId.utf8.value)).update(_.owner -> lift(nft.output.utf8.value))
                          )
                        yield Some(TxEntity.from(txHash, nft, block, blockHash, txJson, fromAccount))
                      case nft: BurnNFT => 
                        val txHash = nft.input.toUInt256Bytes.toHex
                        val newTxEntity: OptionT[EitherT[F, String, *], TxEntity] = 
                          for 
                            // TODO: burnNft 트랜잭션에서는 tokenId가 없어서 이전 트랜잭션 데이터들에서 tokenId 값을 가져와야 하는데 조회에 사용할 필드가 없음.
                            prevNftEntity <- OptionT(NftService.getByTxHash[F](txHash))
                            _ <- OptionT.liftF(upsertTransaction[F, NftTxEntity](
                              query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, prevNftEntity.tokenId, txHash, fromAccount))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                            ))
                          yield TxEntity.from(txHash, nft, prevNftEntity.tokenId, block, blockHash, txJson, fromAccount)
                        newTxEntity.value.recover{(msg: String) =>
                          scribe.info(s"previous nft by txHash: ${txHash} entity doesn't exist in db")
                          None
                        }
                      case nft: EntrustNFT => 
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash, fromAccount))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        ).as(Some(TxEntity.from(txHash, nft, block, blockHash, txJson, fromAccount)))
                      case nft: DisposeEntrustedNFT => 
                        upsertTransaction[F, NftTxEntity](
                          query[NftTxEntity].insertValue(lift(NftTxEntity.from(nft, txHash, fromAccount))).onConflictUpdate(_.txHash)((t, e) => t.txHash -> e.txHash)
                        ).as(Some(TxEntity.from(txHash, nft, block, blockHash, txJson, fromAccount)))
                      
                      case tx: MintFungibleToken => 
                        var sum = 0l;
                        for 
                          _ <- tx.outputs.toList.traverse { case (account, amount) =>
                            sum = sum + amount.toBigInt.longValue
                            updateTransaction[F, AccountEntity](
                              query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                            )
                          }
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                          )
                        yield Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount))
                      case tx: TransferFungibleToken => 
                        var sum = 0l;
                        for 
                          _ <- tx.outputs.toList.traverse { case (account, amount) =>
                            sum = sum + amount.toBigInt.longValue
                            updateTransaction[F, AccountEntity](
                              query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                            )
                          }
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                          )
                        yield Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount))
                      case tx: EntrustFungibleToken => 
                        for 
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(tx.to.utf8.value)).update(a => a.balance -> (a.balance + lift(tx.amount.toBigInt.longValue)))
                          )
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(tx.amount.toBigInt.longValue)))
                          )
                        yield Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount))
                      case tx: DisposeEntrustedFungibleToken => 
                        var sum = 0l;
                        for 
                          _ <- tx.outputs.toList.traverse { case (account, amount) =>
                            sum = sum + amount.toBigInt.longValue
                            updateTransaction[F, AccountEntity](
                              query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                            )
                          }
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                          )
                        yield Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount))
                      case tx: BurnFungibleToken => 
                        updateTransaction[F, AccountEntity](
                          query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(tx.amount.toBigInt.longValue)))
                        ).as(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                    case accountTx: Transaction.AccountTx => accountTx match
                      case tx: CreateAccount => 
                        upsertTransaction[F, AccountEntity](
                          query[AccountEntity].insertValue(lift(AccountEntity.from(tx))).onConflictUpdate(_.address)((t, e) => t.address -> e.address)
                        ).as(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: UpdateAccount => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: AddPublicKeySummaries => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                    case groupTx: Transaction.GroupTx => groupTx match
                      case tx: CreateGroup => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: AddAccounts => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                    case rewardTx: Transaction.RewardTx => rewardTx match
                      case tx: RegisterDao => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: UpdateDao => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: RecordActivity => 
                        EitherT.pure(Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount)))
                      case tx: OfferReward => 
                        var sum = 0l
                        for 
                          _ <- tx.outputs.toList.traverse { case (account, amount) =>
                            sum = sum + amount.toBigInt.longValue
                            updateTransaction[F, AccountEntity](
                              query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                            )
                          }
                          _ <- updateTransaction[F, AccountEntity](
                            query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                          )
                        yield Some(TxEntity.from(txHash, tx, block, blockHash, txJson, fromAccount))
                      case tx: ExecuteReward => 
                        txResult.result.getOrElse(EitherT.pure(None)) match 
                          case rewardRes: ExecuteRewardResult => 
                            var sum = 0l
                            for 
                              _ <- rewardRes.outputs.toList.traverse { case (account, amount) =>
                                sum = sum + amount.toBigInt.longValue
                                updateTransaction[F, AccountEntity](
                                  query[AccountEntity].filter(a => a.address == lift(account.utf8.value)).update(a => a.balance -> (a.balance + lift(amount.toBigInt.longValue)))
                                )
                              }
                              _ <- updateTransaction[F, AccountEntity](
                                query[AccountEntity].filter(a => a.address == lift(fromAccount)).update(a => a.balance -> (a.balance - lift(sum)))
                              )
                            yield Some(TxEntity.from(txHash, tx, rewardRes, block, blockHash, txJson, fromAccount))
                          case _ => EitherT.pure(None)
                      
                  _ <- txEntityOpt match  
                    case Some(value) => {
                      for 
                        _ <- EitherT.right(Async[F].delay(scribe.info(s"update block transaction")))
                        _ <- upsertTransaction[F, TxEntity](query[TxEntity].insertValue(lift(value)).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash))
                        _ <- upsertTransaction[F, BlockEntity](
                          quote { query[BlockEntity].insertValue(lift(BlockEntity.from(block, blockHash))).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash) })
                        _ <- updateTransaction[F, BlockStateEntity](quote { query[BlockStateEntity].filter(b => b.hash == lift(blockHash)).update(_.isBuild -> lift(true)) })
                      yield ()
                    }
                    case None => EitherT.pure[F, String](0L)
                yield txEntityOpt
                result.as(())
                // result.map(_=>())
            } 
          yield ()
        }
        blockWithJson <- blockWithJsonEither.traverse{ EitherT.fromEither(_) }.leftMap(e => e.getMessage())
      yield if blockWithJson.isEmpty then None else Some(blockWithJson.last)


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
            _.number -> lift(block.header.number.toBigInt.longValue),
            _.createdAt -> lift(java.time.Instant.now().getEpochSecond()),
          ).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash)
        )

    //   case false =>
    // def rollbackSavedDiffStates(): IO[Unit] = ???

    def loop[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Unit] =
      for 
        _ <- EitherT.right(Async[F].delay(scribe.info(s"Checking for newly created blocks")))
        status <- getStatus[F](backend)
        _ <- EitherT.right(Async[F].delay(scribe.info(s"status: ${status}")))
        bestBlock <- getBlock[F](backend)(status.bestHash)
        _ <- bestBlock.fold(EitherT.pure(())) { case (block, json) =>
          for
            _ <- EitherT.right(Async[F].delay(scribe.info(s"start ss")))
            // TODO: read sequentially saved last block
            prevLastBlockHash: String <- getLastSavedBlock[F].map {
              case Some(lastBlock) => lastBlock.hash
              case None => status.genesisHash.toUInt256Bytes.toBytes.toHex
            }
            _ <- EitherT.right(Async[F].delay(scribe.info(s"prevLastBlockHash: $prevLastBlockHash")))

            lastSavedBlockOpt <- saveDiffStateLoop[F](backend)(bestBlock, prevLastBlockHash)

            _ <- EitherT.right(Async[F].delay(scribe.info(s"lastSavedBlockOpt: $lastSavedBlockOpt")))

            currLastSavedBlockOpt <- buildSavedStateLoop[F](backend)

            _ <- currLastSavedBlockOpt match
              case Some(currLastSavedBlock) => saveLastSavedBlockLog[F]((currLastSavedBlock))
              case None => EitherT.pure(0L)

          yield ()
        }
            

        _ <- EitherT.right(Async[F].delay(scribe.info(s"New block checking finished.")))
        _ <- EitherT.right(Async[F].sleep(5000.millis))
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
          scribe.info(s"respone: $response")
          for
            body <- response.body
            a <- decode[A](body).leftMap(_.getMessage())  
          yield
            scribe.info(s"yield: $a") 
            Some(a, body)
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

  def summaryLoop[F[_]: Async](backend: SttpBackend[F, Any]): EitherT[F, String, Unit] =
    def loop(backend: SttpBackend[F, Any]): EitherT[F, String, Unit] =
      for 
        _ <- EitherT.right(Async[F].delay(scribe.info(s"summary loop start")))
        coinMarket <- ExternalApiService.getLmPrice(backend)  // coinMarket response

        lastSavedBlockOpt <- BlockService.getLastSavedBlock[F]
        
        _ <- (coinMarket, lastSavedBlockOpt) match { 
          case (Some(data), Some(lastSavedBlock)) => 
            val lmPrice = data.quote.USD
            for 
              txCountInLatest24h <- TxService.countInLatest24h[F]
              totalAccountCnt <- AccountService.totalCount[F]
              _ <- upsertTransaction[F, SummaryEntity](
                query[SummaryEntity].insert(
                  _.lmPrice -> lift(lmPrice.price),
                  _.blockNumber -> lift(lastSavedBlock.number),
                  _.txCountsIn24Hour -> lift(txCountInLatest24h),
                  _.totalAccounts -> lift(totalAccountCnt),
                )
              )
            yield ()
          case _ => EitherT.pure(())
        }
        _ <- EitherT.right(Async[F].sleep(10000.millis))
        _ <- EitherT.right(Async[F].delay(scribe.info(s"summary loop finished")))
      yield ()
    
    loop(backend)


  def run(args: List[String]): IO[ExitCode] =
    def newClientFactory(options: SttpBackendOptions): ClientFactory = {
      val builder = ClientFactory
        .builder()
        .connectTimeoutMillis(options.connectionTimeout.toMillis)
      options.proxy.fold(builder.build()) { proxy =>
        builder
          .proxyConfig(proxy.asJavaProxySelector)
          .build()
      }
    }

    def webClient(options: SttpBackendOptions) = WebClient
      .builder()
      .decorator(
        DecodingClient
          .builder()
          .autoFillAcceptEncoding(false)
          .strictContentEncoding(false)
          .newDecorator()
      )
      .factory(newClientFactory(options))
      .build()

    ArmeriaCatsBackend
      // .resource[IO](SttpBackendOptions.Default)
      .resourceUsingClient[IO](webClient(SttpBackendOptions.Default))
      .use { backend =>
        val program = for
          // _ <- Async[IO].racePair(blockCheckLoop(backend))
          
          _ <- List(
            blockCheckLoop(backend), 
            // summaryLoop(backend)
          ).parSequence
        yield ()
        program.value
    }
    .as(ExitCode.Success)
