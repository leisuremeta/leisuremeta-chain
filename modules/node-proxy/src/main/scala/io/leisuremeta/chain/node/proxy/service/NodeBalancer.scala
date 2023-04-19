package io.leisuremeta.chain.node.proxy
package service

import java.nio.file.{Files, Paths, StandardOpenOption}
import io.leisuremeta.chain.api.model.NodeStatus
import io.leisuremeta.chain.api.model.Block.ops.*
import io.leisuremeta.chain.api.model.Block.*
import io.circe.parser.decode
import io.circe.generic.auto.*
import io.circe.syntax.given
import cats.syntax.*
import cats.syntax.functor._
import cats.implicits.*
import io.leisuremeta.chain.api.model.Block
import sttp.client3.*
import cats.effect.Ref
import cats.data.OptionT
import cats.effect.kernel.Async
import sttp.model.{Uri, StatusCode}
import model.NodeConfig
import io.leisuremeta.chain.lib.crypto.Hash.ops.*
import io.leisuremeta.chain.api.model.Block.blockHash
import fs2.{Stream, text}
import fs2.io.file.Path
import fs2.text.utf8
import scala.util.Try
import scala.concurrent.duration._
import service.NodeWatchService
import cats.syntax.flatMap._
import io.leisuremeta.chain.lib.datatype.BigNat
import io.leisuremeta.chain.lib.crypto.Hash.Value
import io.leisuremeta.chain.api.model.Signed.Tx
import io.leisuremeta.chain.node.proxy.model.TxModel



case class NodeBalancer[F[_]: Async] (
  apiService: InternalApiService[F],
  blocker:    Ref[F, Boolean],
  nodeConfig: NodeConfig,
  blcUrls:    Ref[F, List[String]]
):
  def logDiffTxsLoop(startBlock: Block, endBlockNumber: BigNat): F[Unit] =
    def getTxWithExponentialRetry(txHash: String, retries: Int, delay: FiniteDuration): F[Option[String]] =
      apiService.getTxFromOld(txHash).flatMap { (res: Either[String, TxModel]) =>
        res match
          case Right(txModel) => Async[F].pure(Some(txModel.signedTx))
          case Left(err) if retries > 0 => Async[F].sleep(delay) *> getTxWithExponentialRetry(txHash, retries - 1, delay * 2)
          case _ => Async[F].pure(None)
        
        // if code.isSuccess 
        //   then Async[F].pure(Some(res.body))
        // else if code.isServerError && retries > 0
        //   then Async[F].sleep(delay) 
        //         *> getTxWithExponentialRetry(txHash, retries - 1, delay * 2)
        // else Async[F].pure(None)
      }
      
    def loop(currBlock: Block): F[Unit] = 
      if (currBlock.header.number != endBlockNumber) {
        val parentHash = currBlock.header.parentHash.toUInt256Bytes.toBytes.toHex
        for 
          txList    <- currBlock.transactionHashes.toList.traverse { txHash => 
                         getTxWithExponentialRetry(txHash.toUInt256Bytes.toBytes.toHex, 10, 1.second) }
          _         <- txList.traverse { txOpt =>
                         txOpt match 
                           case Some(tx) => appendLog("diff-txs.json", tx.trim)
                           case None     => Async[F].unit
                       }
          // _         <- appendLog("diff-txs.json", txList.asJson.spaces2)
          prevBlock <- apiService.block(nodeConfig.oldNodeAddress, parentHash)
          _         <- loop(prevBlock)
        yield () 
      } else {
        apiService.bestBlock(nodeConfig.oldNodeAddress).flatMap { newBestBlock =>
          val newBestHashInOld = newBestBlock.toHash.toUInt256Bytes.toBytes.toHex
          val startBlockHash   = startBlock.toHash.toUInt256Bytes.toBytes.toHex 
          if newBestHashInOld != startBlockHash
            then logDiffTxsLoop(newBestBlock, startBlock.header.number)
            else Async[F].unit
        }
      }

    loop(startBlock)

  def appendLog(path: String, json: String): F[Unit] = Async[F].blocking {
    Files.write(
      Paths.get(path),
      (json + "\n").getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )
  }

  def createTxsToNewBlockchain: F[Unit] = 
    val path = Path("diff-txs.json")
    def postTxWithExponentialRetry(line: String, retries: Int, delay: FiniteDuration): F[String] =
      println(s"line: $line")
      apiService.postTx(nodeConfig.newNodeAddress.get, line).flatMap { res =>
        println(s"response: $res")
        val code = res.code
        if code.isSuccess 
          then Async[F].pure(res.body)
        else if (code.isServerError || res.body.isEmpty) && retries > 0
          then Async[F].sleep(delay) 
                *> postTxWithExponentialRetry(line, retries - 1, delay * 2)
        else Async[F].raiseError(new RuntimeException(s"post tx $retries times failure: ${res.code} ${res.body}"))

        // if (res.isEmpty && retries > 0) 
        //   scribe.error(s"error occured while submiting tx(${line}) from old blockhain to new blockchain")
        //   Async[F].sleep(delay) 
        //    *> postTxWithExponentialRetry(line, retries - 1, delay * 2)
        // else 
        //   Async[F].pure(res)
      }
      
    fs2.io.file.Files[F]
      .readAll(path)
      .through(utf8.decode)
      .through(text.lines)
      .evalMap(line => postTxWithExponentialRetry(s"[$line]", 100, 1.second))
      .compile
      .drain

  def deleteAllFiles: F[Unit] = Async[F].blocking {
    val diffTxs = Paths.get("diff-txs.json")
    Files.deleteIfExists(diffTxs)

    // val diffBlocks = Paths.get("diff-blocks.json")
    // Files.deleteIfExists(diffBlocks)    
  }

  def run(): F[Unit] =
    def loop(endBlockNumber: BigNat): F[Unit] = 
      val newNode = nodeConfig.newNodeAddress.get
      scribe.info(s"newNode: ${newNode}")
      val oldNode = nodeConfig.oldNodeAddress
      scribe.info(s"oldNode: ${oldNode}")
      scribe.info(s"endBlockNumber: ${endBlockNumber}")
      for 
        // bestBlock in old blockchain
        startBlock <- apiService.bestBlock(oldNode)
        _          <- Async[F].delay(println(s"startBlock: ${startBlock}"))
        _          <- if startBlock.header.number == endBlockNumber
                        then blocker.set(true) // TODO: Lock 을 언제 푸는지?
                      else 
                        println("NodeBalancer.loop() else branch")
                        scribe.info("NodeBalancer.loop() else branch")
                        // logDiffTxsLoop(startBlock, endBlockNumber)
                        //  *> createTxsToNewBlockchain
                        createTxsToNewBlockchain
                        //  *> deleteAllFiles
                        //  *> loop(startBlock.header.number)
      yield ()

    loop(nodeConfig.blockNumber)

  