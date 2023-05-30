package io.leisuremeta.chain
package bulkinsert

import scala.io.Source

import cats.data.{EitherT, Kleisli, StateT}
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*

import com.typesafe.config.{Config, ConfigFactory}
import fs2.Stream
import io.circe.generic.auto.*
import io.circe.parser.decode
import scodec.bits.ByteVector

import api.model.{Block, Signed, StateRoot}
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
import lib.merkle.{MerkleTrie, MerkleTrieState}
import lib.merkle.MerkleTrie.NodeStore
import node.{NodeConfig, NodeMain}
import node.dapp.{PlayNommDApp, PlayNommDAppFailure, PlayNommState}
import node.repository.{BlockRepository, StateRepository, TransactionRepository}
import node.repository.StateRepository.given
import node.service.NodeInitializationService

def bulkInsert[F[_]
  : Async: BlockRepository: TransactionRepository: StateRepository: PlayNommState: InvalidTxLogger](
    config: NodeConfig,
    source: Source,
    offset: Long,
): EitherT[F, String, Unit] = for
  bestBlock <- NodeInitializationService
    .initialize[F](config.genesis.timestamp)
  merkleState = MerkleTrieState.fromRootOption(bestBlock.header.stateRoot.main)
  indexWithTxsStream = Stream
    .fromIterator[EitherT[F, String, *]](source.getLines(), 1)
    .zipWithIndex
    .filterNot(_._1 === "[]")
    .drop(offset)
    .evalMap: (line, index) =>
      EitherT
        .fromEither[F]:
          decode[Seq[Signed.Tx]](line)
        .leftMap: e =>
          scribe.error(s"Error decoding line #$index: $line: $e")
          e.getMessage()
        .map(txs => (index, txs))
  stateStream = indexWithTxsStream.evalMapAccumulate(merkleState):
    case (ms, (index, txs)) =>
      val localKeyPair: KeyPair =
        val privateKey = scala.sys.env
          .get("LMNODE_PRIVATE_KEY")
          .map(BigInt(_, 16))
          .orElse(config.local.`private`)
          .get
        CryptoOps.fromPrivate(privateKey)

      val program = for
        result <- Stream
          .fromIterator[EitherT[F, PlayNommDAppFailure, *]](txs.iterator, 1)
          .evalMapAccumulate(ms): (ms, tx) =>
            scribe.info(s"signer: ${tx.sig.account}")
            scribe.info(s"tx: ${tx.value}")
            PlayNommDApp[F](tx)
              .run(ms)
              .recoverWith: e =>
                RecoverTx(e, ms, tx)
          .map: result =>
            scribe.info(s"#$index: ${result._1.root}")
            result
          .compile
          .toList
          .leftMap: e =>
            scribe.error(s"Error building txs #$index: $txs: $e")
            e
        (states, txWithResults) = result.unzip
        txHashes                = txWithResults.map(_.toHash)
        txState = txs
          .map(_.toHash)
          .sortBy(_.toUInt256Bytes.toBytes)
          .foldLeft(MerkleTrieState.empty): (state, txHash) =>
            given idNodeStore: NodeStore[cats.Id] = Kleisli.pure(None)
            MerkleTrie
              .put[cats.Id](
                txHash.toUInt256Bytes.toBytes.bits,
                ByteVector.empty,
              )
              .runS(state)
              .value
              .getOrElse(state)
        _ <- txWithResults.traverse: txWithResult =>
          EitherT.liftF:
            TransactionRepository[F].put(txWithResult)
      yield (states.last, (index, txWithResults))

      program.leftMap: e =>
        scribe.error(s"Error applying txs #$index: $txs: $e")
        e.msg
  result <- stateStream.last.compile.toList
yield
  scribe.info(s"Last: ${result.flatten}")
  ()

def fileResource[F[_]: Async](fileName: String): Resource[F, Source] =
  Resource.fromAutoCloseable:
    Async[F].delay(Source.fromFile(fileName))

object BulkInsertMain extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    NodeConfig
      .load[IO](IO.blocking(ConfigFactory.load))
      .value
      .flatMap:
        case Left(err) =>
          IO(println(err)).as(ExitCode.Error)
        case Right(config) =>
          scribe.info(s"Loaded config: $config")

          val program = for
            source                          <- fileResource[IO]("txs.archive")
            given BlockRepository[IO]       <- NodeMain.getBlockRepo
            given TransactionRepository[IO] <- NodeMain.getTransactionRepo
            given StateRepository[IO]       <- NodeMain.getStateRepo
            given InvalidTxLogger[IO] <- InvalidTxLogger.file[IO]:
              "invalid-txs.csv"
            given PlayNommState[IO] = PlayNommState.build[IO]
            result <- Resource.eval:
              bulkInsert[IO](config, source, 0).value.map:
                case Left(err) =>
                  scribe.error(s"Error: $err")
                  ExitCode.Error
                case Right(_) =>
                  scribe.info(s"Done")
                  ExitCode.Success
          yield result

          program.use(IO.pure)
