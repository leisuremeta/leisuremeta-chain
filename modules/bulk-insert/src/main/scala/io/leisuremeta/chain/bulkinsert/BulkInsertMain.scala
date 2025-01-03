package io.leisuremeta.chain
package bulkinsert

import scala.io.Source

import cats.data.{EitherT, Kleisli}
import cats.effect.{Async, ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*

import fs2.Stream
import io.circe.parser.decode
import scodec.bits.ByteVector

import api.model.{Block, Signed, StateRoot}
import api.model.TransactionWithResult.ops.*
import lib.crypto.{CryptoOps, KeyPair}
import lib.crypto.Hash.ops.*
import lib.crypto.Sign.ops.*
import lib.datatype.BigNat
import lib.merkle.*
import lib.merkle.MerkleTrie.NodeStore
import node.NodeConfig
import node.dapp.{PlayNommDApp, PlayNommDAppFailure, PlayNommState}
import node.repository.{BlockRepository, StateRepository, TransactionRepository}
import node.service.NodeInitializationService

def bulkInsert[F[_]
  : Async: BlockRepository: TransactionRepository: StateRepository: PlayNommState: InvalidTxLogger](
    config: NodeConfig,
    source: Source,
    from: String,
    until: String,
): EitherT[F, String, Unit] = for
  bestBlock <- NodeInitializationService
    .initialize[F](config.genesis.timestamp)
  merkleState = MerkleTrieState.fromRootOption(bestBlock.header.stateRoot.main)
  indexWithTxsStream = Stream
    .fromIterator[EitherT[F, String, *]](source.getLines(), 1)
    .filterNot(_ === "[]")
    .zipWithIndex
    .evalMap: (line, index) =>
      if index % 1000L === 0L then scribe.info(s"Processing line #$index")
      line.split("\t").toList match
        case blockNumber :: txHash :: jsonString :: Nil =>
          EitherT
            .fromEither[F]:
              decode[Signed.Tx](jsonString)
            .leftMap: e =>
              scribe.error(s"Error decoding line #$blockNumber: $txHash: $jsonString: $e")
              e.getMessage()
            .map(tx => (blockNumber, tx))
        case _ =>
          scribe.error(s"Error parsing line: $line")
          EitherT.leftT[F, (String, Signed.Tx)](s"Error parsing line: $line")
    .groupAdjacentBy(_._1)
    .dropWhile(_._1 =!= from)
    .takeWhile(_._1 =!= until)
    .map:
      case (blockNumber, chunk) =>
        (blockNumber, chunk.toList.map(_._2))
  localKeyPair: KeyPair =
    val privateKey = scala.sys.env
      .get("LMNODE_PRIVATE_KEY")
      .map(BigInt(_, 16))
      .orElse(config.local.`private`)
      .get
    CryptoOps.fromPrivate(privateKey)
  stateStream = indexWithTxsStream.evalMapAccumulate((bestBlock, merkleState)):
    case ((previousBlock, ms), (blockNumber, txs)) =>
      val program = for
        result <- Stream
          .fromIterator[EitherT[F, PlayNommDAppFailure, *]](txs.iterator, 1)
          .evalMapAccumulate(ms): (ms, tx) =>
//            scribe.info(s"signer: ${tx.sig.account}")
//            scribe.info(s"tx: ${tx.value}")
//            PlayNommDApp[F](tx)
//              .run(ms)
//              .map: (ms, txWithResult) =>
//                (ms, Option(txWithResult))
//              .recoverWith: _ =>
//                RecoverTx(ms, tx)
            RecoverTx(ms, tx)
              .recoverWith: failure =>
                PlayNommDApp[F](tx)
                  .run(ms)
                  .map: (ms, txWithResult) =>
                    (ms, Option(txWithResult))
                  .leftMap: failure2 =>
                    scribe.info(s"Error: $failure")
                    failure2
          .map: result =>
            if BigInt(blockNumber) % 100 === 0 then scribe.info(s"#$blockNumber: ${result._1.root}")
            result
          .compile
          .toList
          .leftMap: e =>
            scribe.error(s"Error building txs #$blockNumber: $txs: $e")
            e
          .leftSemiflatTap: e =>
            StateRepository[F]
              .put(ms)
              .leftMap: f =>
                scribe.error(s"Fail to put state: ${f.msg}")
              .value
        (states, txWithResultOptions) = result.unzip
        finalState                    = states.last
        txWithResults                 = txWithResultOptions.flatten
        txHashes                      = txWithResults.map(_.toHash)
        txState = txs
          .map(_.toHash)
          .sortBy(_.toUInt256Bytes.toBytes)
          .foldLeft(MerkleTrieState.empty): (state, txHash) =>
            given idNodeStore: NodeStore[cats.Id] = Kleisli.pure(None)
            MerkleTrie
              .put[cats.Id](
                txHash.toUInt256Bytes.toBytes.toNibbles,
                ByteVector.empty,
              )
              .runS(state)
              .value
              .getOrElse(state)
        stateRoot1 = StateRoot(finalState.root)
        now        = (previousBlock.header.timestamp :: txs.map(_.value.createdAt))
          .maxBy(_.getEpochSecond())
        blockNumber = BigNat.add(previousBlock.header.number, BigNat.One)
        header = Block.Header(
          number = blockNumber,
          parentHash = previousBlock.toHash,
          stateRoot = stateRoot1,
          transactionsRoot = txState.root,
          timestamp = now,
        )
        sig <- EitherT
          .fromEither(header.toHash.signBy(localKeyPair))
          .leftMap: msg =>
            scribe.error(s"Fail to sign header: $msg")
            PlayNommDAppFailure.internal(s"Fail to sign header: $msg")
        block = Block(
          header = header,
          transactionHashes = txHashes.toSet.map(_.toSignedTxHash),
          votes = Set(sig),
        )
        _ <- BlockRepository[F]
          .put(block)
          .leftMap: e =>
            scribe.error(s"Fail to put block: $e")
            PlayNommDAppFailure.internal(s"Fail to put block: ${e.msg}")
        finalState1 <-
          if blockNumber.toBigInt % 10000 === 0 then          
            StateRepository[F]
              .put(finalState)
              .leftMap: e =>
                scribe.error(s"Fail to put state: $e")
                PlayNommDAppFailure.internal:
                  s"Fail to put state: ${e.msg}"
              .map: _ =>
                MerkleTrieState.fromRootOption(finalState.root)
          else EitherT.pure(finalState)
        _ <- txWithResults.traverse: txWithResult =>
          EitherT.liftF:
            TransactionRepository[F].put(txWithResult)
      yield ((block, finalState1), (blockNumber, txWithResults))

      program
        .leftMap: e =>
          scribe.error(s"Error applying txs #$blockNumber: $txs: $e")
          e.msg
  result <- stateStream.last.compile.toList
  finalState <- EitherT.fromOption[F](
    result.headOption.flatten.map(_._1._2),
    "Fail to get final state",
  )
  _ <- StateRepository[F]
    .put(finalState)
    .leftMap: e =>
      scribe.error(s"Fail to put state: $e")
      s"Fail to put state: ${e.msg}"
yield
  scribe.info(s"Last: ${result.flatten}")
  ()

def fileResource[F[_]: Async](fileName: String): Resource[F, Source] =
  Resource.fromAutoCloseable:
    Async[F].delay(Source.fromFile(fileName))


object BulkInsertMain extends IOApp:

  val from = "1"
//  val from = "3513172"
 
  val until =   "100000000"
//  val until = "3513173"
  
  override def run(args: List[String]): IO[ExitCode] =

    import com.typesafe.config.ConfigFactory
    import node.NodeMain
    import node.repository.StateRepository.given

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
            given BlockRepository[IO]       <- NodeMain.getBlockRepo(config)
            given TransactionRepository[IO] <- NodeMain.getTransactionRepo(config)
            given StateRepository[IO]       <- NodeMain.getStateRepo(config)
            given InvalidTxLogger[IO] <- InvalidTxLogger.file[IO]:
              "invalid-txs.csv"
            result <- Resource.eval:
              given PlayNommState[IO] = PlayNommState.build[IO]
              bulkInsert[IO](config, source, from, until).value.map:
                case Left(err) =>
                  scribe.error(s"Error: $err")
                  ExitCode.Error
                case Right(_) =>
                  scribe.info(s"Done")
                  ExitCode.Success
          yield result

          program.use(IO.pure)

//          fileResource[IO]("txs.archive")
//            .use: source =>
//              NftBalanceState.build(source).flatTap: state =>
//                IO.pure:
//                  state.free.foreach(println)
//                  state.locked.foreach(println)
//              FungibleBalanceState.build(source).flatTap: state =>
//                IO.pure:
//                  state.free.foreach(println)
//                  state.locked.foreach(println)
//            .as(ExitCode.Success)
