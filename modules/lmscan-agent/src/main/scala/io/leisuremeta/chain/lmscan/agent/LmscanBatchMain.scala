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

object LmscanBatchMain extends IOApp:
  val baseUri = "http://localhost:8081"

  val initalBlock = ""

  def run(args: List[String]): IO[ExitCode] =
    for _ <- ArmeriaCatsBackend
        .resource[IO](SttpBackendOptions.Default)
        .use { backend =>
          for
            status <- getStatus[IO](backend)
            block  <- getBlock[IO](backend)(status.bestHash)
            count <- loop[IO](backend)(
              status.bestHash,
              status.genesisHash,
              0,
            ) {}
          yield ()
          // val unsavedBlocks = readUnsavedBlocks()
          // val lastReadBlock = getLastBlockRead()
          // lastReadBlock.map {

          //   block <- getBlockListFrom(backend, .)

          // }
          // getTx()
          IO.unit
        }
    yield ExitCode.Success

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

  def loop[F[_]: Async](
      backend: SttpBackend[F, Any],
  ): EitherT[F, String, Long] =
    ???

  def getStatus[F[_]: Async](
      backend: SttpBackend[F, Any],
  ): EitherT[F, String, NodeStatus] =
    get[F, NodeStatus](backend) {
      uri"$baseUri/status"
    }

  def getBlock[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(blockHash: String): EitherT[F, String, PBlock] =
    get[F, PBlock](backend) {
      uri"$baseUri/block/${blockHash}"
    }

  def getBlockListFrom[F[_]: Async](
      backend: SttpBackend[F, Any],
  )(blockHash: String): EitherT[F, String, Seq[BlockInfo]] =
    get[F, Seq[BlockInfo]](backend) {
      uri"$baseUri/block?from=${blockHash}"
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
