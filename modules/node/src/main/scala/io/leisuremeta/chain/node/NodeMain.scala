package io.leisuremeta.chain
package node

import java.nio.file.{Path, Paths}
import java.time.Instant

import cats.effect.{ExitCode, IO, IOApp}
//import cats.effect.unsafe.implicits.global
import com.typesafe.config.{Config, ConfigFactory}

import api.{LeisureMetaChainApi as Api}
import api.model.*
import api.model.account.EthAddress
import api.model.reward.*
import api.model.token.*
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import lib.merkle.MerkleTrieNode
import lib.merkle.MerkleTrieNode.{MerkleHash, MerkleRoot}
import repository.{BlockRepository, StateRepository, TransactionRepository}
import repository.StateRepository.given
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

    type StateRepoStore[F[_], K, V] =
      StoreIndex[IO, MerkleHash[K, V], MerkleTrieNode[K, V]]

    def getStateRepo[K: ByteCodec, V: ByteCodec](
        dir: Path,
    ): IO[StateRepoStore[IO, K, V]] =
      sway[MerkleHash[K, V], MerkleTrieNode[K, V]](dir)

    def getTransactionRepo: IO[TransactionRepository[IO]] =
      for given StoreIndex[IO, Hash.Value[
          TransactionWithResult,
        ], TransactionWithResult] <-
          sway[Hash.Value[TransactionWithResult], TransactionWithResult](
            Paths.get("sway", "transaction"),
          )
      yield TransactionRepository.fromStores[IO]

    val getConfig: IO[Config] = IO.blocking(ConfigFactory.load)

    NodeConfig.load[IO](getConfig).value.flatMap {
      case Right(config) =>
        for
          given BlockRepository[IO] <- getBlockRepo
          given StateRepoStore[IO, Account, AccountData] <-
            getStateRepo[Account, AccountData](
              Paths.get("sway", "state", "account", "name"),
            )
          given StateRepoStore[
            IO,
            (Account, PublicKeySummary),
            PublicKeySummary.Info,
          ] <-
            getStateRepo[(Account, PublicKeySummary), PublicKeySummary.Info](
              Paths.get("sway", "state", "account", "pubkey"),
            )
          given StateRepoStore[IO, EthAddress, Account] <-
            getStateRepo[EthAddress, Account](Paths.get("sway", "state", "account", "eth"))
          given StateRepoStore[IO, GroupId, GroupData] <-
            getStateRepo[GroupId, GroupData](
              Paths.get("sway", "state", "group"),
            )
          given StateRepoStore[IO, (GroupId, Account), Unit] <-
            getStateRepo[(GroupId, Account), Unit](
              Paths.get("sway", "state", "group", "account"),
            )
          given StateRepoStore[IO, TokenDefinitionId, TokenDefinition] <-
            getStateRepo[TokenDefinitionId, TokenDefinition](
              Paths.get("sway", "state", "token", "definition"),
            )
          given StateRepoStore[
            IO,
            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
            Unit,
          ] <-
            getStateRepo[
              (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
              Unit,
            ](
              Paths.get("sway", "state", "token", "fungible-balance"),
            )
          given StateRepoStore[
            IO,
            (Account, TokenId, Hash.Value[TransactionWithResult]),
            Unit,
          ] <-
            getStateRepo[
              (Account, TokenId, Hash.Value[TransactionWithResult]),
              Unit,
            ](
              Paths.get("sway", "state", "token", "nft-balance"),
            )
          given StateRepoStore[IO, TokenId, NftState] <-
            getStateRepo[TokenId, NftState](
              Paths.get("sway", "state", "token", "nft"),
            )
          given StateRepoStore[
            IO,
            (TokenDefinitionId, Rarity, TokenId),
            Unit,
          ] <-
            getStateRepo[(TokenDefinitionId, Rarity, TokenId), Unit](
              Paths.get("sway", "state", "token", "rarity"),
            )
          given StateRepoStore[
            IO,
            (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
            Unit,
          ] <-
            getStateRepo[
              (Account, Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
              Unit,
            ](
              Paths.get("sway", "state", "token", "entrust-fungible-balance"),
            )
          given StateRepoStore[
            IO,
            (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
            Unit,
          ] <-
            getStateRepo[
              (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
              Unit,
            ](
              Paths.get("sway", "state", "token", "entrust-nft-balance"),
            )
          given StateRepoStore[IO, GroupId, DaoInfo] <-
            getStateRepo[GroupId, DaoInfo](
              Paths.get("sway", "state", "reward", "dao"),
            )
          given StateRepoStore[IO, (Instant, Account), DaoActivity] <-
            getStateRepo[(Instant, Account), DaoActivity](
              Paths.get("sway", "state", "reward", "user-activity"),
            )
          given StateRepoStore[IO, (Instant, TokenId), DaoActivity] <-
            getStateRepo[(Instant, TokenId), DaoActivity](
              Paths.get("sway", "state", "reward", "token-received"),
            )
          given TransactionRepository[IO] <- getTransactionRepo

          appResource <- NodeApp[IO](config).resource
          exitcode    <- appResource.useForever.as(ExitCode.Success)
        yield exitcode
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
