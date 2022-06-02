package io.leisuremeta.chain
package node

import java.nio.file.{Path, Paths}

import cats.effect.{ExitCode, IO, IOApp}
//import cats.effect.unsafe.implicits.global
import com.typesafe.config.{Config, ConfigFactory}

import api.{LeisureMetaChainApi as Api}
import api.model.*
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import lib.merkle.MerkleTrieNode
import lib.merkle.MerkleTrieNode.{MerkleHash, MerkleRoot}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import service.LocalGossipService
import service.interpreter.LocalGossipServiceInterpreter
import store.*
import store.interpreter.StoreIndexSwayInterpreter
object NodeMain extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =

    def sway[K: ByteCodec, V: ByteCodec](dir: Path): IO[StoreIndex[IO, K, V]] =
      StoreIndexSwayInterpreter[K, V](dir)

    def getBlockRepo: IO[BlockRepository[IO]] = for
      bestBlockKVStore <- sway[UInt256Bytes, Block.Header](
        Paths.get("sway", "block", "best"),
      )
      given SingleValueStore[IO, Block.Header] = SingleValueStore
        .fromKeyValueStore[IO, Block.Header](bestBlockKVStore)
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

    def getStateRepo[K: ByteCodec, V: ByteCodec](
        dir: Path,
    ): IO[StateRepository[IO, K, V]] =
      for given StoreIndex[
          IO,
          MerkleHash[K, V],
          (MerkleTrieNode[K, V], BigNat),
        ] <-
          sway[MerkleHash[K, V], (MerkleTrieNode[K, V], BigNat)](dir)
      yield StateRepository.fromStores[IO, K, V]

    def getTransactionRepo: IO[TransactionRepository[IO]] = for
      given StoreIndex[IO, Hash.Value[TransactionWithResult], TransactionWithResult] <- 
        sway[Hash.Value[TransactionWithResult], TransactionWithResult](Paths.get("sway", "transaction"))
    yield TransactionRepository.fromStores[IO]


    val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

    NodeConfig.load[IO](getConfig).value.flatMap {
      case Right(config) =>
        
        for
          given BlockRepository[IO] <- getBlockRepo
          given StateRepository[IO, Account, Option[Account]] <-
            getStateRepo[Account, Option[Account]](
              Paths.get("sway", "state", "name"),
            )
          given StateRepository[
            IO,
            (Account, PublicKeySummary),
            PublicKeySummary.Info,
          ] <-
            getStateRepo[(Account, PublicKeySummary), PublicKeySummary.Info](
              Paths.get("sway", "state", "pubkey"),
            )
          given StateRepository[IO, GroupId, GroupData] <-
            getStateRepo[GroupId, GroupData](Paths.get("sway", "state", "group"))
          given StateRepository[IO, (GroupId, Account), Unit] <-
            getStateRepo[(GroupId, Account), Unit](Paths.get("sway", "state", "group", "account"))
          given TransactionRepository[IO] <- getTransactionRepo

          appResource <- NodeApp[IO](config).resource
          exitcode <- appResource.useForever.as(ExitCode.Success)
        yield exitcode
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
