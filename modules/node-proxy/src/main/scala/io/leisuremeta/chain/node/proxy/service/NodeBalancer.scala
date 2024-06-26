package io.leisuremeta.chain.node.proxy
package service

import java.nio.file.{Files, Paths, StandardOpenOption}
import cats.implicits.*
import io.leisuremeta.chain.api.model.Block
import cats.effect.Ref
import cats.effect.kernel.Async
import sttp.model.StatusCode
import model.NodeConfig
import fs2.io.file.Path
import fs2.text.utf8
import scala.concurrent.duration._
import io.leisuremeta.chain.lib.datatype.BigNat
import io.leisuremeta.chain.node.proxy.model.TxModel
import fs2.Chunk

import fs2.{Stream, text}
import fs2.Pipe

case class NodeBalancer[F[_]: Async] (
  apiService:   InternalApiService[F],
  blocker:      Ref[F, Boolean],
  baseUrlsLock: Ref[F, List[String]],
  nodeConfig:   NodeConfig,
  queue:        PostTxQueue[F]
):
  // startBlock: bestBlock in old blockchain
  def logDiffTxsLoop(startBlock: Block, endBlockNumber: BigNat): F[Unit] =
    def getTxWithExponentialRetry(txHash: String, retries: Int, delay: FiniteDuration): F[Option[String]] =
      apiService.getTxFromOld(txHash).flatMap { (_, res) =>
        res match
          case Right(txModel) => Async[F].pure(Some(txModel.signedTx))
          case Left(err) if retries > 0 => Async[F].sleep(delay) 
                                            *> getTxWithExponentialRetry(txHash, retries - 1, delay * 2)
          case _ => Async[F].pure(None)
      }
      
    def loop(currBlock: Block): F[Unit] = 
      if (currBlock.header.number != endBlockNumber) {
        println(s"block download number: ${currBlock.header.number}")
        val parentHash = currBlock.header.parentHash.toUInt256Bytes.toBytes.toHex
        for 
          txList    <- currBlock.transactionHashes.toList.traverse { txHash => 
                         getTxWithExponentialRetry(txHash.toUInt256Bytes.toBytes.toHex, 5, 1.second) }
          filteredTxList = txList.flatMap(identity)
          _         <- filteredTxList match 
                      case head :: tail => appendLog("diff-txs.json", s"[${filteredTxList.mkString(",")}]")
                      case Nil => Async[F].unit
          res       <- apiService.block(nodeConfig.oldNodeAddress, parentHash)
          (_, prevBlock) = res
          _         <- loop(prevBlock)
        yield ()
      } else {
        scribe.info("logDiffTxsLoop 종료")
        Async[F].unit
      }

    loop(startBlock)

  def appendLog(path: String, json: String): F[Unit] = Async[F].blocking:
    val _ = java.nio.file.Files.write(
      Paths.get(path),
      (json + "\n").getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )
  
  def postTxWithExponentialRetry(line: String, retries: Int, delay: FiniteDuration): F[String] =
    println(s"request txs: $line")
    apiService.postTx(nodeConfig.newNodeAddress.get, line).flatMap { (_, res) => //(statusCode, res) =>
      println(s"generated hash: $res")
      val code = res.code
      if code.isSuccess 
        then Async[F].pure(res.body)
      else if (code.isServerError || res.body.isEmpty) && retries > 0
        then Async[F].sleep(delay) 
              *> postTxWithExponentialRetry(line, retries - 1, delay * 2)
      else Async[F].pure("")
    }

  def createTxsToNewBlockchain: F[Option[String]] = 
    val path = fs2.io.file.Path("diff-txs.json")

    def processLinesReversed(): F[Option[String]] = 
      def reverseLines(): F[Option[String]] = 
        def reversePipe: Pipe[F, String, String] = _.flatMap { s =>
          Stream.chunk(Chunk.from(s.split('\n').toVector.reverse))
        }.fold(Vector.empty[String]) { (acc, line) =>
          line +: acc
        }.flatMap(Stream.emits)

        fs2.io.file.Files.forAsync[F]
          .readAll(path)
          .through(fs2.text.utf8.decode)
          .through(text.lines)
          .through(reversePipe)
          .scan((Option.empty[String], Option.empty[String])) { case ((_, prev), txs) =>
            if (txs.isBlank) (None, prev)
            else (Some(txs), Some(txs))
          }
          .evalMap { case (txsOpt, lastTxs) => 
            txsOpt match 
              case None => Async[F].pure(lastTxs)
              case Some(txs) => postTxWithExponentialRetry(txs, 5, 1.second).as(lastTxs)
          }
          .last
          .compile
          .lastOrError
          .map(_.flatten)
          .flatMap(Async[F].pure)

      reverseLines()
    processLinesReversed()


  // def createTxsToNewBlockchain1[F[_]: Async](implicit C: Concurrent[F], console: Console[F]): F[Unit] = 
  //   val path = Paths.get("diff-txs.json")

  //   def reverseFile(): F[Unit] =
  //     Stream.eval(fs2.io.file.Files[F].size(path)).flatMap { size =>
  //       Stream.unfoldLoopEval(size - 1L) { offset =>
  //         Async[F].uncancelable { _ =>
  //           fs2.io.file.Files[F].readRange(path, 1, offset, offset + 1)
  //             .through(fs2.text.utf8.decode)
  //             .compile
  //             .string
  //             .map { char =>
  //               (char, if (char.nonEmpty) Some(offset - 1L) else None)
  //             }
  //         }
  //       }
  //     }
  //     .through(fs2.text.utf8.encode)
  //     .through(fs2.text.utf8.decode)
  //     .through(text.lines)
  //     .evalMap { txs =>
  //       if (txs.isBlank) Async[F].unit
  //       else postTxWithExponentialRetry(txs, 5, 1.second).as(())
  //     }
  //     .last
  //     .compile
  //     .lastOrError

  //   reverseFile()
      

  def deleteAllFiles: F[Unit] = Async[F].blocking:
    val diffTxs = Paths.get("diff-txs.json")
    val _ = Files.deleteIfExists(diffTxs)

  def run(): F[Unit] =
    def loop(endBlockNumber: BigNat, lastTxsOpt: Option[String]): F[Unit] = 
      val newNodeAddr = nodeConfig.newNodeAddress.get
      val oldNodeAddr = nodeConfig.oldNodeAddress
      scribe.info(s"[oldNode: $oldNodeAddr]", s"-> [newNode: $newNodeAddr]")
      for 
        response   <- apiService.bestBlock(oldNodeAddr)
        (_, startBlock) = response
        _          <- if (startBlock.header.number == endBlockNumber) {
                        for 
                          _ <- blocker.set(false)
                          _ <- queue.pollsAfter(lastTxsOpt).flatMap { jsons => 
                                 jsons.traverse { txs => postTxWithExponentialRetry(txs, 5, 1.second)}
                               } 
                          _ <- baseUrlsLock.getAndUpdate{_.appended(nodeConfig.newNodeAddress.get)} 
                          _ <- blocker.set(true) // 양쪽 모두 릴레이 시작.
                          _ <- Async[F].delay(scribe.info("마이그레이션 성공. 양쪽 모두 API 릴레이 시작"))
                          newNodeCfg <- NodeWatchService.waitTerminateSig
                          _ <- Async[F].delay(scribe.info(s"now api request only relayed to ${newNodeCfg.oldNodeAddress}"))
                          _ <- baseUrlsLock.set(List(newNodeCfg.oldNodeAddress))
                        yield ()
                      } else {
                        logDiffTxsLoop(startBlock, endBlockNumber)
                        >> createTxsToNewBlockchain.flatMap { lastTxs => 
                          val validLastTxs = lastTxs match
                            case Some(lastTxs) => Some(lastTxs)
                            case None => lastTxsOpt
                          deleteAllFiles
                          >> loop(startBlock.header.number, validLastTxs) 
                        }
                      }
      yield ()

    loop(nodeConfig.blockNumber.getOrElse(
          throw new NoSuchElementException("local migration 이 완료된 blockNumber를 적어주세요.")),
         None)

  