package io.leisuremeta.chain
package node
package repository

import java.time.Instant

import cats.{Functor, Monad}
import cats.data.{EitherT, Kleisli, OptionT}
import cats.implicits.*

import api.model.{
  Account,
  AccountData,
  GroupId,
  GroupData,
  PublicKeySummary,
  TransactionWithResult,
}
import api.model.dao.{DaoData}
import api.model.token.{NftState, Rarity, TokenDefinition, TokenDefinitionId, TokenId}
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.{MerkleTrie, MerkleTrieNode, MerkleTrieState}
import lib.merkle.MerkleTrie.NodeStore
import lib.merkle.MerkleTrieNode.{MerkleHash, MerkleRoot}
import lib.failure.DecodingFailure
import store.KeyValueStore

trait StateRepository[F[_], K, V]:
  def get(
      merkleRoot: MerkleRoot[K, V],
  ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]]

  def put(state: MerkleTrieState[K, V]): F[Unit]

object StateRepository:

  /** AccountState */
  trait AccountState[F[_]]:
    def name: StateRepository[F, Account, AccountData]
    def key
        : StateRepository[F, (Account, PublicKeySummary), PublicKeySummary.Info]
  object AccountState:
    def apply[F[_]: AccountState]: AccountState[F] = summon
    given from[F[_]: Monad](using
        nameKVStroe: MerkleHashStore[F, Account, AccountData],
        keyKVStroe: MerkleHashStore[
          F,
          (Account, PublicKeySummary),
          PublicKeySummary.Info,
        ],
    ): AccountState[F] = new AccountState[F]:
      def name: StateRepository[F, Account, AccountData] = fromStores
      def key: StateRepository[
        F,
        (Account, PublicKeySummary),
        PublicKeySummary.Info,
      ] = fromStores

  given nodeStoreFromAccount[F[_]: Functor: AccountState]
      : NodeStore[F, Account, AccountData] =
    Kleisli(AccountState[F].name.get(_).leftMap(_.msg))
  given nodeStoreFromAccountKey[F[_]: Functor: AccountState]
      : NodeStore[F, (Account, PublicKeySummary), PublicKeySummary.Info] =
    Kleisli(AccountState[F].key.get(_).leftMap(_.msg))

  /** GroupState */
  trait GroupState[F[_]]:
    def group: StateRepository[F, GroupId, GroupData]
    def groupAccount: StateRepository[F, (GroupId, Account), Unit]
  object GroupState:
    def apply[F[_]: GroupState]: GroupState[F] = summon
    given from[F[_]: Monad](using
        groupKVStroe: MerkleHashStore[F, GroupId, GroupData],
        groupAccountKVStroe: MerkleHashStore[
          F,
          (GroupId, Account),
          Unit,
        ],
    ): GroupState[F] = new GroupState[F]:
      def group: StateRepository[F, GroupId, GroupData] = fromStores
      def groupAccount: StateRepository[F, (GroupId, Account), Unit] =
        fromStores

  given nodeStoreFromGroup[F[_]: Functor: GroupState]
      : NodeStore[F, GroupId, GroupData] =
    Kleisli(GroupState[F].group.get(_).leftMap(_.msg))
  given nodeStoreFromGroupAccount[F[_]: Functor: GroupState]
      : NodeStore[F, (GroupId, Account), Unit] =
    Kleisli(GroupState[F].groupAccount.get(_).leftMap(_.msg))

  /** TokenState */
  trait TokenState[F[_]]:
    def definition: StateRepository[F, TokenDefinitionId, TokenDefinition]
    def fungibleBalance: StateRepository[
      F,
      (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
      Unit,
    ]
    def nftBalance: StateRepository[
      F,
      (Account, TokenId, Hash.Value[TransactionWithResult]),
      Unit,
    ]
    def nft: StateRepository[F, TokenId, NftState]
    def rarity: StateRepository[F, (TokenDefinitionId, Rarity, TokenId), Unit]
    def lock: StateRepository[F, (Account, Hash.Value[TransactionWithResult]), Unit]
    def deadline: StateRepository[F, (Instant, Hash.Value[TransactionWithResult]), Unit]
    def suggestion: StateRepository[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit]

  object TokenState:
    def apply[F[_]: TokenState]: TokenState[F] = summon
    given from[F[_]: Monad](using
        difinitionKVStroe: MerkleHashStore[
          F,
          TokenDefinitionId,
          TokenDefinition,
        ],
        fungibleBalanceKVStroe: MerkleHashStore[
          F,
          (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
          Unit,
        ],
        nftBalanceKVStore: MerkleHashStore[
          F,
          (Account, TokenId, Hash.Value[TransactionWithResult]),
          Unit,
        ],
        nftKVStore: MerkleHashStore[F, TokenId, NftState],
        rarityKVStore: MerkleHashStore[F, (TokenDefinitionId, Rarity, TokenId), Unit],
        lockKVStore: MerkleHashStore[F, (Account, Hash.Value[TransactionWithResult]), Unit],
        deadlineKVStore: MerkleHashStore[F, (Instant, Hash.Value[TransactionWithResult]), Unit],
        suggestionKVStore: MerkleHashStore[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit],
    ): TokenState[F] = new TokenState[F]:
      def definition: StateRepository[F, TokenDefinitionId, TokenDefinition] =
        fromStores
      def fungibleBalance: StateRepository[
        F,
        (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
        Unit,
      ] = fromStores
      def nftBalance: StateRepository[
        F,
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ] = fromStores
      def nft: StateRepository[F, TokenId, NftState] = fromStores
      def rarity: StateRepository[F, (TokenDefinitionId, Rarity, TokenId), Unit] =
        fromStores
      def lock: StateRepository[F, (Account, Hash.Value[TransactionWithResult]), Unit] =
        fromStores
      def deadline: StateRepository[F, (Instant, Hash.Value[TransactionWithResult]), Unit] = fromStores
      def suggestion: StateRepository[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit] =
        fromStores


  given nodeStoreFromDefinition[F[_]: Functor: TokenState]
      : NodeStore[F, TokenDefinitionId, TokenDefinition] =
    Kleisli(TokenState[F].definition.get(_).leftMap(_.msg))

  given nodeStoreFromFungibleBalance[F[_]: Functor: TokenState]: NodeStore[
    F,
    (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
    Unit,
  ] =
    Kleisli(TokenState[F].fungibleBalance.get(_).leftMap(_.msg))

  given nodeStoreFromNftBalance[F[_]: Functor: TokenState]
      : NodeStore[F, (Account, TokenId, Hash.Value[TransactionWithResult]), Unit] =
    Kleisli(TokenState[F].nftBalance.get(_).leftMap(_.msg))
  
  given nodeStoreFromNft[F[_]: Functor: TokenState]
      : NodeStore[F, TokenId, NftState] =
    Kleisli(TokenState[F].nft.get(_).leftMap(_.msg))
  
  given nodeStoreFromRarity[F[_]: Functor: TokenState]
      : NodeStore[F, (TokenDefinitionId, Rarity, TokenId), Unit] =
    Kleisli(TokenState[F].rarity.get(_).leftMap(_.msg))

  given nodeStoreFromLock[F[_]: Functor: TokenState]
      : NodeStore[F, (Account, Hash.Value[TransactionWithResult]), Unit] =
    Kleisli(TokenState[F].lock.get(_).leftMap(_.msg))
  given nodeStoreFromDeadline[F[_]: Functor: TokenState]
      : NodeStore[F, (Instant, Hash.Value[TransactionWithResult]), Unit] =
    Kleisli(TokenState[F].deadline.get(_).leftMap(_.msg))
  given nodeStoreFromSuggestion[F[_]: Functor: TokenState]
      : NodeStore[F, (Hash.Value[TransactionWithResult], Hash.Value[TransactionWithResult]), Unit] =
    Kleisli(TokenState[F].suggestion.get(_).leftMap(_.msg))

  /** DaoState */
  trait DaoState[F[_]]:
    def dao: StateRepository[F, GroupId, DaoData]
  object DaoState:
    def apply[F[_]: DaoState]: DaoState[F] = summon
    given from[F[_]: Monad](using
        groupKVStroe: MerkleHashStore[F, GroupId, DaoData],
    ): DaoState[F] = new DaoState[F]:
      def dao: StateRepository[F, GroupId, DaoData] = fromStores

  given nodeStoreFromDao[F[_]: Functor: DaoState]
    : NodeStore[F, GroupId, DaoData] =
    Kleisli(DaoState[F].dao.get(_).leftMap(_.msg))
  
  /** RandomOffering */
  trait RandomOfferingState[F[_]]:
    def randomOffering: StateRepository[F, TokenDefinitionId, Hash.Value[TransactionWithResult]]
  object RandomOfferingState:
    def apply[F[_]: RandomOfferingState]: RandomOfferingState[F] = summon
    given from[F[_]: Monad](using
        randomOfferingKVStroe: MerkleHashStore[F, TokenDefinitionId, Hash.Value[TransactionWithResult]],
    ): RandomOfferingState[F] = new RandomOfferingState[F]:
      def randomOffering: StateRepository[F, TokenDefinitionId, Hash.Value[TransactionWithResult]] = fromStores

  given nodeStoreFromRandomOffering[F[_]: Functor: RandomOfferingState]
      : NodeStore[F, TokenDefinitionId, Hash.Value[TransactionWithResult]] =
    Kleisli(RandomOfferingState[F].randomOffering.get(_).leftMap(_.msg))

  /** General */
  given nodeStore[F[_]: Functor, K, V](using
      sr: StateRepository[F, K, V],
  ): NodeStore[F, K, V] = Kleisli(sr.get(_).leftMap(_.msg))

  type MerkleHashStore[F[_], K, V] =
    KeyValueStore[F, MerkleHash[K, V], (MerkleTrieNode[K, V], BigNat)]

  def fromStores[F[_]: Monad, K, V](using
      stateKvStore: MerkleHashStore[F, K, V],
  ): StateRepository[F, K, V] = new StateRepository[F, K, V]:

    def get(
        merkleRoot: MerkleRoot[K, V],
    ): EitherT[F, DecodingFailure, Option[MerkleTrieNode[K, V]]] =
      OptionT(stateKvStore.get(merkleRoot)).map(_._1).value

    def put(state: MerkleTrieState[K, V]): F[Unit] = for
      _ <- Monad[F].pure(scribe.debug(s"Putting state: $state"))
      _ <- state.diff.toList.traverse { case (hash, (node, count)) =>
        stateKvStore.get(hash).value.flatMap {
          case Right(Some((node0, count0))) =>
            val count1 = count0.toBigInt.toLong + count
            if count1 <= 0 then stateKvStore.remove(hash)
            else if count > 0 then
              stateKvStore.put(hash, (node, BigNat.unsafeFromLong(count1)))
            else Monad[F].unit
          case Right(None) =>
            if count > 0 then
              stateKvStore.put(hash, (node, BigNat.unsafeFromLong(count)))
            else Monad[F].unit
          case Left(err) => throw new Exception(s"Error: $err")
        }
      }
      _ <- Monad[F].pure(scribe.debug(s"Putting completed: $state"))
    yield ()
