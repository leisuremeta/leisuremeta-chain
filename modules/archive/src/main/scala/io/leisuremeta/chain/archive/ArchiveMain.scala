package io.leisuremeta.chain
package archive

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.Instant

import scala.concurrent.duration.*
//import scala.io.Source

import cats.Monad
import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.bifunctor.*
//import cats.syntax.eq.*
//import cats.syntax.flatMap.toFlatMapOps
import cats.syntax.functor.*
import cats.syntax.traverse.*

import io.circe.generic.auto.*
import io.circe.parser.decode
//import io.circe.refined.*
import io.circe.syntax.*

import sttp.client3.*
import sttp.client3.armeria.cats.ArmeriaCatsBackend
import sttp.model.Uri

import api.model.*
import lib.crypto.{Hash, Signature}
import lib.datatype.*

final case class PBlock(
    header: PHeader,
    transactionHashes: Set[Signed.TxHash],
    votes: Set[Signature],
)
final case class PHeader(
    number: BigNat,
    parentHash: Block.BlockHash,
    timestamp: Instant,
)

object ArchiveMain extends IOApp:

//  val baseUri = "http://test.chain.leisuremeta.io:8080"
//  val baseUri = "http://localhost:7080"
  val baseUri = "http://localhost:8081"

  val archiveFileName = "txs1.archive"

  def logTxs(contents: String): IO[Unit] = IO.blocking:
    val path = Paths.get(archiveFileName)
    val _ = Files.write(
      path,
      contents.getBytes,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.APPEND,
    )

  def get[F[_]: Monad, A: io.circe.Decoder](
      backend: SttpBackend[F, Any],
  )(uri: Uri): EitherT[F, String, A] = EitherT:
    basicRequest
      .get(uri)
      .send(backend)
      .map: response =>
        for
          body <- response.body
          a    <- decode[A](body).leftMap(_.getMessage())
        yield a

  def put[F[_]: Monad](
      backend: SttpBackend[F, Any],
  )(uri: Uri)(line: String): EitherT[F, String, List[Signed.TxHash]] =
    EitherT:
//      println(s"Req: $line")

      basicRequest
        .post(uri)
        .body(line)
        .send(backend)
        .map: response =>
//          println(s"Response: $response")
          val result = for
            body <- response.body
            a    <- decode[List[Signed.TxHash]](body).leftMap(_.getMessage())
          yield a

          result

  def getTransaction[F[_]: Monad](
      backend: SttpBackend[F, Any],
  )(txHash: Signed.TxHash): EitherT[F, String, TransactionWithResult] =
    get[F, TransactionWithResult](backend)
      .apply:
        uri"$baseUri/tx/${txHash.toUInt256Bytes.toBytes.toHex}"
      .leftMap: msg =>
        scribe.error(s"error msg: $msg")
        msg

  def getBlock[F[_]: Monad](
      backend: SttpBackend[F, Any],
  )(blockHash: Block.BlockHash): EitherT[F, String, PBlock] =
    get[F, PBlock](backend):
      uri"$baseUri/block/${blockHash.toUInt256Bytes.toBytes.toHex}"

  def getStatus[F[_]: Monad](
      backend: SttpBackend[F, Any],
  ): EitherT[F, String, NodeStatus] =
    get[F, NodeStatus](backend)(uri"$baseUri/status")

  def loop[F[_]: Monad](
      backend: SttpBackend[F, Any],
  )(next: Block.BlockHash, genesis: Block.BlockHash, count: Long)(
      run: (
          BigNat,
          Block.BlockHash,
          Set[Signed.TxHash],
      ) => EitherT[F, String, Unit],
  ): EitherT[F, String, Long] = for
    block <- getBlock[F](backend)(next)
    _     <- EitherT.pure(scribe.info(s"block ${block.header.number}: $next"))
    _ <- run(block.header.number, next, block.transactionHashes).recover: msg =>
      scribe.error(s"error msg: $msg")
      ()
    count1 <- loop[F](backend)(block.header.parentHash, genesis, count + 1)(run)
  yield count1

  def run(args: List[String]): IO[ExitCode] =
    for _ <- ArmeriaCatsBackend
        .resource[IO]:
          SttpBackendOptions.Default.connectionTimeout(10.minutes)
        .use: backend =>
          val program = for
            status <- getStatus[IO](backend)
            block  <- getBlock[IO](backend)(status.bestHash)
            count <- loop[IO](backend)(
              status.bestHash,
              status.genesisHash,
              0,
            ): (blockNumber, blockHash, txSet) =>
              txSet.toList
                .sortBy(_.toUInt256Bytes.toHex)
                .traverse: txHash =>
                  for
                    tx <- getTransaction[IO](backend)(txHash)
                    txString = tx.signedTx.asJson.noSpaces
                    _ <- EitherT.right:
                      logTxs:
                        s"$blockNumber\t${txHash.toUInt256Bytes.toHex}\t$txString\n"
                  yield ()
                .as(())
          yield println(s"total number of block: $count")

//        val from = 0
//        val to = 1000000
//        Source.fromFile(archiveFileName).getLines.to(LazyList).zipWithIndex.take(to).drop(from).traverse{
//          (line, i) =>
////            println(s"$i: $line")
//            put[IO](backend)(uri"$baseUri/tx")(line).recover{
//              case msg: String =>
//                println(s"Error: $msg")
//                println(s"Error Request: $line")
//                Nil
//            }
//        }
          program.value
    yield ExitCode.Success
