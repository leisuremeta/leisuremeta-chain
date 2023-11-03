package io.leisuremeta.chain
package node

import java.time.Instant

import cats.effect.{ExitCode, Resource, IO, IOApp}
import com.typesafe.config.{Config, ConfigFactory}

import api.{LeisureMetaChainApi as Api}
import api.model.*
import dapp.PlayNommState
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import lib.merkle.MerkleTrieNode
import lib.merkle.MerkleTrieNode.MerkleHash
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository.given
import store.*
import store.interpreter._
import io.leisuremeta.chain.lib.failure.DecodingFailure
import cats.data.EitherT

object NodeMain extends IOApp:
  def multi[K: ByteCodec, V: ByteCodec](
      config: NodeConfig,
      target: InterpreterTarget,
  ): Resource[IO, KeyValueStore[IO, K, V]] =
    MultiInterpreter[K, V](config.redis, target)

  def getBlockRepo(config: NodeConfig): Resource[IO, BlockRepository[IO]] = for
    bestBlockKVStore <- multi[UInt256Bytes, Block.Header](config, InterpreterTarget.BEST_NUM)
    given SingleValueStore[IO, Block.Header] = SingleValueStore
      .fromKeyValueStore[IO, Block.Header](using bestBlockKVStore)
    given KeyValueStore[IO, Block.BlockHash, Block] <- multi[Hash.Value[
      Block,
    ], Block](config, InterpreterTarget.BLOCK)
    given KeyValueStore[IO, BigNat, Block.BlockHash] <- multi[
      BigNat,
      Block.BlockHash,
    ](config, InterpreterTarget.BLOCK_NUM)
    given KeyValueStore[IO, Signed.TxHash, Block.BlockHash] <- multi[
      Signed.TxHash,
      Block.BlockHash,
    ](config, InterpreterTarget.TX_BLOCK)
  yield BlockRepository.fromStores[IO]

  def getStateRepo(config: NodeConfig): Resource[IO, StateRepository[IO]] = for given KeyValueStore[
      IO,
      MerkleHash,
      MerkleTrieNode,
    ] <- multi[MerkleHash, MerkleTrieNode](config, InterpreterTarget.MERKLE_TRIE)
  yield StateRepository.fromStores[IO]

  def getTransactionRepo(config: NodeConfig): Resource[IO, TransactionRepository[IO]] =
    for given KeyValueStore[IO, Hash.Value[
        TransactionWithResult,
      ], TransactionWithResult] <-
        multi[Hash.Value[TransactionWithResult], TransactionWithResult](config, InterpreterTarget.TX)
    yield TransactionRepository.fromStores[IO]

  override def run(args: List[String]): IO[ExitCode] =

    val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

    NodeConfig.load[IO](getConfig).value.flatMap {
      case Right(config) =>
        val program = for
          given BlockRepository[IO]       <- getBlockRepo(config)
          given TransactionRepository[IO] <- getTransactionRepo(config)
          given StateRepository[IO]       <- getStateRepo(config)
          given PlayNommState[IO] = PlayNommState.build[IO]
          server <- NodeApp[IO](config).resource
        yield server

        program.use(_ => IO.never).as(ExitCode.Success)
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
