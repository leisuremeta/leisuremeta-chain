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
import fs2.Pull
import fs2.Chunk
import java.nio.charset.StandardCharsets
import java.io.RandomAccessFile
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.channels.Channels
import java.io.{RandomAccessFile, InputStreamReader}
import cats.effect.Concurrent

import fs2.{Stream, text}
import fs2.{Chunk, Pipe, Stream}

case class NodeBalancer[F[_]: Async] (
  apiService: InternalApiService[F],
  blocker:    Ref[F, Boolean],
  nodeConfig: NodeConfig,
):
  def logDiffTxsLoop(startBlock: Block, endBlockNumber: BigNat): F[Unit] =
    def getTxWithExponentialRetry(txHash: String, retries: Int, delay: FiniteDuration): F[Option[String]] =
      println("111111")
      apiService.getTxFromOld(txHash).flatMap { res =>
        println("22222")
        res match
          case Right(txModel) => Async[F].pure(Some(txModel.signedTx))
          case Left(err) if retries > 0 => Async[F].sleep(delay) 
                                            *> getTxWithExponentialRetry(txHash, retries - 1, delay * 2)
          case _ => Async[F].pure(None)
      }
      
    def loop(currBlock: Block): F[Unit] = 
      if (currBlock.header.number != endBlockNumber) {
        val parentHash = currBlock.header.parentHash.toUInt256Bytes.toBytes.toHex
        for 
          txList    <- currBlock.transactionHashes.toList.traverse { txHash => 
                         println(s"---txHash: $txHash")
                         getTxWithExponentialRetry(txHash.toUInt256Bytes.toBytes.toHex, 10, 1.second) }
          _         <- Async[F].delay{scribe.info(txList.toString())}
          _         <- txList.traverse { txOpt =>
                         txOpt match 
                           case Some(tx) => appendLog("diff-txs.json", tx.trim)
                           case None     => Async[F].unit
                       }
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
    java.nio.file.Files.write(
      Paths.get(path),
      (json + "\n").getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )
  }

  def createTxsToNewBlockchain: F[Unit] = 
    val path = Paths.get("diff-txs.json")
    val charset = Charset.forName("UTF-8")

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
      }

          
    def processLinesReversed(): F[Unit] = 
      def reverseLines(): F[Unit] = 
        def reversePipe: Pipe[F, String, String] = _.flatMap { s =>
          Stream.chunk(Chunk.vector(s.split('\n').toVector.reverse))
        }.fold(Vector.empty[String]) { (acc, line) =>
          line +: acc
        }.flatMap(Stream.emits)

        fs2.io.file.Files[F]
          .readAll(fs2.io.file.Path.fromNioPath(path))
          .through(fs2.text.utf8.decode)
          .through(text.lines)
          .through(reversePipe)
          .evalMap(line => 
            if !line.isBlank() 
              then postTxWithExponentialRetry(s"[$line]", 5, 1.second) >> Async[F].unit
            else 
              Async[F].unit
          )
          .compile
          .drain

      reverseLines()
    processLinesReversed()

    // fs2.io.file.Files[F]
    //   .readAll(path)
    //   .through(utf8.decode)
    //   .through(text.lines)
    //   .evalMap(line => postTxWithExponentialRetry(s"[$line]", 100, 1.second))
    //   .compile
    //   .drain

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
                        scribe.info("NodeBalancer.loop() else branch")
                        // logDiffTxsLoop(startBlock, endBlockNumber)
                        createTxsToNewBlockchain
                        //  >> deleteAllFiles
                        //  >> loop(startBlock.header.number)
                        // createTxsToNewBlockchain
                        //  *> deleteAllFiles
                        //  *> loop(startBlock.header.number)
      yield ()
    loop(nodeConfig.blockNumber)

  