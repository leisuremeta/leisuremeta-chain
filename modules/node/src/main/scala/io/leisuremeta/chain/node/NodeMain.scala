package io.leisuremeta.chain
package node

import java.nio.file.{Path, Paths}
import java.time.Instant

import cats.effect.{ExitCode, Resource, IO, IOApp}
//import cats.effect.unsafe.implicits.global
import com.typesafe.config.{Config, ConfigFactory}

import api.{LeisureMetaChainApi as Api}
import api.model.*
import api.model.account.EthAddress
import api.model.reward.*
import api.model.token.*
import dapp.PlayNommState
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import lib.merkle.MerkleTrieNode
import lib.merkle.MerkleTrieNode.MerkleHash
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository.given
import store.*
import store.interpreter.StoreIndexSwayInterpreter

object NodeMain extends IOApp:
  def sway[K: ByteCodec, V: ByteCodec](
      dir: Path,
  ): Resource[IO, StoreIndex[IO, K, V]] =
    StoreIndexSwayInterpreter[K, V](dir)

  def getBlockRepo: Resource[IO, BlockRepository[IO]] = for
    bestBlockKVStore <- sway[UInt256Bytes, Block.Header](
      Paths.get("sway", "block", "best"),
    )
    given SingleValueStore[IO, Block.Header] = SingleValueStore
      .fromKeyValueStore[IO, Block.Header](using bestBlockKVStore)
    given StoreIndex[IO, Block.BlockHash, Block] <- sway[Hash.Value[
      Block,
    ], Block](Paths.get("sway", "block"))
    given StoreIndex[IO, BigNat, Block.BlockHash] <- sway[
      BigNat,
      Block.BlockHash,
    ](Paths.get("sway", "block", "number"))
    given StoreIndex[IO, Signed.TxHash, Block.BlockHash] <- sway[
      Signed.TxHash,
      Block.BlockHash,
    ](Paths.get("sway", "block", "tx"))
  yield BlockRepository.fromStores[IO]

  def getStateRepo: Resource[IO, StateRepository[IO]] = for given KeyValueStore[
      IO,
      MerkleHash,
      MerkleTrieNode,
    ] <- sway[MerkleHash, MerkleTrieNode](Paths.get("sway", "state"))
  yield StateRepository.fromStores[IO]

  def getTransactionRepo: Resource[IO, TransactionRepository[IO]] =
    for given StoreIndex[IO, Hash.Value[
        TransactionWithResult,
      ], TransactionWithResult] <-
        sway[Hash.Value[TransactionWithResult], TransactionWithResult](
          Paths.get("sway", "transaction"),
        )
    yield TransactionRepository.fromStores[IO]

  override def run(args: List[String]): IO[ExitCode] =

    val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

    NodeConfig.load[IO](getConfig).value.flatMap {
      case Right(config) =>
        val program = for
          given BlockRepository[IO]       <- getBlockRepo
          given TransactionRepository[IO] <- getTransactionRepo
          given StateRepository[IO]       <- getStateRepo
          given PlayNommState[IO] = PlayNommState.build[IO]
          server <- NodeApp[IO](config).resource
        yield server

        program.use(_ => IO.never).as(ExitCode.Success)
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
