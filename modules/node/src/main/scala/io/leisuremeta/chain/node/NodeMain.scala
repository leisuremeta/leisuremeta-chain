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
import dapp.PlayNommState
import lib.codec.byte.ByteCodec
import lib.crypto.Hash
import lib.datatype.{BigNat, UInt256Bytes}
import lib.merkle.MerkleTrieNode
import lib.merkle.MerkleTrieNode.MerkleHash
import repository.{
  BlockRepository,
//  GenericStateRepository,
  StateRepository,
  TransactionRepository,
}
//import repository.GenericStateRepository.given
import repository.StateRepository.given
//import service.LocalGossipService
//import service.interpreter.LocalGossipServiceInterpreter
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

//    type StateRepoStore[F[_], K, V] =
//      StoreIndex[IO, GenericMerkleHash[K, V], GenericMerkleTrieNode[K, V]]

//    def getGenericStateRepo[K: ByteCodec, V: ByteCodec](
//        dir: Path,
//    ): IO[StateRepoStore[IO, K, V]] =
//      sway[GenericMerkleHash[K, V], GenericMerkleTrieNode[K, V]](dir)

    def getStateRepo: IO[StateRepository[IO]] = for given KeyValueStore[
        IO,
        MerkleHash,
        MerkleTrieNode,
      ] <- sway[MerkleHash, MerkleTrieNode](Paths.get("sway", "state"))
    yield StateRepository.fromStores[IO]

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
//          given StateRepoStore[IO, Account, AccountData] <-
//            getGenericStateRepo[Account, AccountData](
//              Paths.get("sway", "state", "account", "name"),
//            )
//          given StateRepoStore[
//            IO,
//            (Account, PublicKeySummary),
//            PublicKeySummary.Info,
//          ] <-
//            getGenericStateRepo[
//              (Account, PublicKeySummary),
//              PublicKeySummary.Info,
//            ](
//              Paths.get("sway", "state", "account", "pubkey"),
//            )
//          given StateRepoStore[IO, EthAddress, Account] <-
//            getGenericStateRepo[EthAddress, Account](
//              Paths.get("sway", "state", "account", "eth"),
//            )
//          given StateRepoStore[IO, GroupId, GroupData] <-
//            getGenericStateRepo[GroupId, GroupData](
//              Paths.get("sway", "state", "group"),
//            )
//          given StateRepoStore[IO, (GroupId, Account), Unit] <-
//            getGenericStateRepo[(GroupId, Account), Unit](
//              Paths.get("sway", "state", "group", "account"),
//            )
//          given StateRepoStore[IO, TokenDefinitionId, TokenDefinition] <-
//            getGenericStateRepo[TokenDefinitionId, TokenDefinition](
//              Paths.get("sway", "state", "token", "definition"),
//            )
//          given StateRepoStore[
//            IO,
//            (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
//            Unit,
//          ] <-
//            getGenericStateRepo[
//              (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
//              Unit,
//            ](
//              Paths.get("sway", "state", "token", "fungible-balance"),
//            )
//          given StateRepoStore[
//            IO,
//            (Account, TokenId, Hash.Value[TransactionWithResult]),
//            Unit,
//          ] <-
//            getGenericStateRepo[
//              (Account, TokenId, Hash.Value[TransactionWithResult]),
//              Unit,
//            ](
//              Paths.get("sway", "state", "token", "nft-balance"),
//            )
//          given StateRepoStore[IO, TokenId, NftState] <-
//            getGenericStateRepo[TokenId, NftState](
//              Paths.get("sway", "state", "token", "nft"),
//            )
//          given StateRepoStore[
//            IO,
//            (TokenDefinitionId, Rarity, TokenId),
//            Unit,
//          ] <-
//            getGenericStateRepo[(TokenDefinitionId, Rarity, TokenId), Unit](
//              Paths.get("sway", "state", "token", "rarity"),
//            )
//          given StateRepoStore[
//            IO,
//            (
//                Account,
//                Account,
//                TokenDefinitionId,
//                Hash.Value[TransactionWithResult],
//            ),
//            Unit,
//          ] <-
//            getGenericStateRepo[
//              (
//                  Account,
//                  Account,
//                  TokenDefinitionId,
//                  Hash.Value[TransactionWithResult],
//              ),
//              Unit,
//            ](
//              Paths.get("sway", "state", "token", "entrust-fungible-balance"),
//            )
//          given StateRepoStore[
//            IO,
//            (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
//            Unit,
//          ] <-
//            getGenericStateRepo[
//              (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
//              Unit,
//            ](
//              Paths.get("sway", "state", "token", "entrust-nft-balance"),
//            )
//          given StateRepoStore[IO, GroupId, DaoInfo] <-
//            getGenericStateRepo[GroupId, DaoInfo](
//              Paths.get("sway", "state", "reward", "dao"),
//            )
//          given StateRepoStore[IO, (Instant, Account), DaoActivity] <-
//            getGenericStateRepo[(Instant, Account), DaoActivity](
//              Paths.get("sway", "state", "reward", "user-activity"),
//            )
//          given StateRepoStore[IO, (Instant, TokenId), DaoActivity] <-
//            getGenericStateRepo[(Instant, TokenId), DaoActivity](
//              Paths.get("sway", "state", "reward", "token-received"),
//            )
          given TransactionRepository[IO] <- getTransactionRepo
          given StateRepository[IO]       <- getStateRepo
          given PlayNommState[IO] = PlayNommState.build[IO]

          appResource <- NodeApp[IO](config).resource
          exitcode    <- appResource.useForever.as(ExitCode.Success)
        yield exitcode
      case Left(err) =>
        IO(println(err)).as(ExitCode.Error)
    }
